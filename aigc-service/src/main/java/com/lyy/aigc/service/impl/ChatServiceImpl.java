package com.lyy.aigc.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lyy.aigc.config.SystemPromptConfig;
import com.lyy.aigc.config.ToolResultHolder;
import com.lyy.aigc.constants.Constant;
import com.lyy.aigc.entity.dto.ChatDTO;
import com.lyy.aigc.entity.vo.ChatEventVO;
import com.lyy.aigc.service.ChatService;
import com.lyy.aigc.service.ChatSessionService;
import com.lyy.common.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private SystemPromptConfig systemPromptConfig;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatSessionService chatSessionService;
    //通过一个容器，保存sessionId以及是否继续生成的标识
//    private static final Map<String, Boolean> sessionIdMap = new ConcurrentHashMap<>();
    private static final String STATUS_KEY = "chat:status:";


    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        var conversationId = ChatService.getConversationId(chatDTO.getSessionId());

        //RAG顾问
        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d) // 设置相似度阈值
                        .topK(5) // 搜索结果数量
                        .build()
                )
                .build();

        chatSessionService.update(chatDTO.getSessionId(), chatDTO.getQuestion(), BaseContext.getCurrentId());
        // 用于保存停止输出的记录
        StringBuilder stopHistoryContent = new StringBuilder();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(STATUS_KEY);
        hashOps.put(chatDTO.getSessionId(), "true");
        //生成请求id
        var requestId = IdUtil.fastSimpleUUID();
        return this.chatClient.prompt()
                .system(promptSystem ->promptSystem
                        .text(systemPromptConfig.getChatSystemMessage().get())
                        )
                .advisors(advisor -> advisor
                        .advisors(qaAdvisor)
                        .param(ChatMemory.CONVERSATION_ID,conversationId ))//设置对话记忆中的对话id
                .user(chatDTO.getQuestion())
                .toolContext(Map.of(Constant.REQUEST_ID, requestId))//将请求id存入工具容器
                .stream()
                .chatResponse()
                .doFirst(() -> hashOps.put(chatDTO.getSessionId(), "true"))
                .doOnComplete(() -> log.info("Chat completed {}", chatDTO.getSessionId()))
                .doOnError(throwable -> {hashOps.delete(chatDTO.getSessionId());log.error("Chat error {}", chatDTO.getSessionId(), throwable);})
                .doOnCancel(() -> this.saveStopHistoryRecord(conversationId, stopHistoryContent.toString()))//手动停止时将停止输出的记录保存起来
                .doOnComplete(() -> hashOps.delete(chatDTO.getSessionId()))//对话完成时删除会话缓存标识
                .takeWhile(chatResponse -> hashOps.get(chatDTO.getSessionId()) != null)
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    stopHistoryContent.append(text);//手动添加停止输出的记录
                    String messageId = chatResponse.getMetadata().getId();
                    ToolResultHolder.put(messageId,Constant.REQUEST_ID, requestId);//将请求id存入工具容器
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(1001)
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    Map<String, Object> result = ToolResultHolder.get(requestId);
                    if (result != null) {
                        ToolResultHolder.remove(requestId);
                        //工具被调用，需要对前端传递参数
                        return Flux.just(ChatEventVO.builder()
                                        .eventData(result)
                                        .eventType(1003)
                                        .build()// 工具调用参数
                                , ChatEventVO.builder()
                                        .eventType(1002)
                                        .build());// 工具调用结束
                    }
                    return Flux.just(ChatEventVO.builder()
                            .eventType(1002)
                            .build());// 添加完成标记
                }));
    }

    @Override
    public void stop(String sessionId) {
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(STATUS_KEY);
        hashOps.delete(sessionId);
    }
    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }
}
