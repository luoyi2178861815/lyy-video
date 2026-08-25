package com.lyy.aigc.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lyy.aigc.config.QueryTransformerConfig;
import com.lyy.aigc.config.SystemPromptConfig;
import com.lyy.aigc.config.ToolResultHolder;
import com.lyy.aigc.constants.Constant;
import com.lyy.aigc.entity.dto.ChatDTO;
import com.lyy.aigc.entity.vo.ChatEventVO;
import com.lyy.aigc.service.ChatService;
import com.lyy.aigc.service.ChatSessionService;
import com.lyy.common.context.BaseContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private QueryTransformerConfig queryTransformerConfig;
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
    // RAG 检索参数
    private static final double SIMILARITY_THRESHOLD = 0.6d;
    private static final int TOP_K = 5;


    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        var conversationId = ChatService.getConversationId(chatDTO.getSessionId());
        Query originalQuery = new Query(chatDTO.getQuestion());
        Query transformedQuery = queryTransformerConfig.transform(originalQuery);
        // 手动 RAG 检索 + 观测日志
        String context = retrieveWithObservability(transformedQuery.text());

        chatSessionService.update(chatDTO.getSessionId(), chatDTO.getQuestion(), BaseContext.getCurrentId());
        // 用于保存停止输出的记录
        StringBuilder stopHistoryContent = new StringBuilder();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(STATUS_KEY);
        hashOps.put(chatDTO.getSessionId(), "true");
        //生成请求id
        var requestId = IdUtil.fastSimpleUUID();
        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(buildSystemMessage(context))
                        )
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, conversationId))//设置对话记忆中的对话id
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

    /**
     * 手动执行 RAG 检索并记录观测日志（耗时、召回数、相似度分布）
     *
     * @param question 用户问题
     * @return 拼接后的检索上下文，无结果时返回空字符串
     */
    private String retrieveWithObservability(String question) {
        long start = System.currentTimeMillis();
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .topK(TOP_K)
                        .build());
        long cost = System.currentTimeMillis() - start;

        if (documents.isEmpty()) {
            log.info("[RAG观测] query={}, 检索耗时={}ms, 召回=0条", question, cost);
            return "";
        }

        List<Double> scores = documents.stream()
                .map(Document::getScore)
                .filter(Objects::nonNull)
                .toList();

        log.info("[RAG观测] query={}, 检索耗时={}ms, 召回={}条, 相似度={}",
                question, cost, documents.size(), scores);

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            context.append("[").append(i + 1).append("] ")
                    .append(documents.get(i).getText()).append("\n");
        }
        return context.toString();
    }

    /**
     * 组装 System Prompt：基础系统提示词 + 检索上下文
     */
    private String buildSystemMessage(String context) {
        String basePrompt = systemPromptConfig.getChatSystemMessage().get();
        if (context == null || context.isBlank()) {
            return basePrompt;
        }
        return basePrompt + "\n\n参考知识库内容（仅基于以下内容回答，不要编造）：\n" + context;
    }
}
