package com.lyy.aigc.service.impl;

import com.lyy.aigc.config.SystemPromptConfig;
import com.lyy.aigc.entity.dto.ChatDTO;
import com.lyy.aigc.entity.vo.ChatEventVO;
import com.lyy.aigc.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatClient chatClient;
    @Autowired
    private SystemPromptConfig systemPromptConfig;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //通过一个容器，保存sessionId以及是否继续生成的标识
//    private static final Map<String, Boolean> sessionIdMap = new ConcurrentHashMap<>();
    private static final String STATUS_KEY = "chat:status:";

    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        var conversationId = ChatService.getConversationId(chatDTO.getSessionId());
        // 用于保存停止输出的记录
        StringBuilder stopHistoryContent = new StringBuilder();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(STATUS_KEY);
        hashOps.put(chatDTO.getSessionId(), "true");
        return this.chatClient.prompt()
                .system(promptSystem ->promptSystem
                        .text(systemPromptConfig.getChatSystemMessage().get())
                        )
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID,conversationId ))//设置对话记忆中的对话id
                .user(chatDTO.getQuestion())
                .stream()
                .chatResponse()
                .doFirst(() -> {
                    hashOps.put(chatDTO.getSessionId(), "true");
                })
                .doOnComplete(() -> {log.info("Chat completed {}", chatDTO.getSessionId());})
                .doOnError(throwable -> {hashOps.delete(chatDTO.getSessionId());log.error("Chat error {}", chatDTO.getSessionId(), throwable);})
                .doOnCancel(() ->{
                    this.saveStopHistoryRecord(conversationId, stopHistoryContent.toString());
                })
                .doOnComplete(() ->{hashOps.delete(chatDTO.getSessionId());})
                .takeWhile(chatResponse -> hashOps.get(chatDTO.getSessionId()) != null)
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    stopHistoryContent.append(text);
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(1001)
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventType(1002)
                        .build()));// 添加完成标记
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
