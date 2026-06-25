package com.lyy.aigc.config;

import com.lyy.aigc.memory.RedisChatMemoryRepository;
import com.lyy.aigc.tools.VideoTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.chat.max-messages}")
    private int maxMessages;

    /**
     * 配置 ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 Advisor loggerAdvisor, Advisor messageChatMemoryAdvisor, VideoTools videoTool) {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(videoTool);
        return chatClientBuilder
                .defaultAdvisors(loggerAdvisor, messageChatMemoryAdvisor)
                .defaultToolCallbacks(toolCallbacks)
                .build();
    }

    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * 配置 RedisChatMemoryRepository
     */
    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }

    /**
     * 配置 ChatMemory
     */
    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    /**
     * 配置 MessageChatMemoryAdvisor用于
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

}
