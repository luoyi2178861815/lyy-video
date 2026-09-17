package com.lyy.aigc.memory;

import cn.hutool.core.stream.StreamUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

@Slf4j
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    public static final String DEFAULT_PREFIX = "chat:";
    public RedisChatMemoryRepository() {
        this(DEFAULT_PREFIX);
    }

    public RedisChatMemoryRepository(String prefix) {
        PREFIX = prefix;
        log.info("RedisChatMemoryRepository 被创建, prefix: {}, 实例: {}", prefix, this);
    }

    private  final String PREFIX ;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @NotNull
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(PREFIX + "*");
        return StreamUtil.of(keys).map(key -> key.replace(PREFIX, "")).toList();
    }

    @NotNull
    @Override
    public List<Message> findByConversationId(@NotNull String conversationId) {
        var redisKey = this.getKey(conversationId);
        var messages = stringRedisTemplate.boundListOps(redisKey).range(0, -1);
        if (messages == null){
            return List.of();
        }
        return StreamUtil.of(messages).map(MessageUtil::toMessage).toList();
    }

    @Override
    public void saveAll(@NotNull String conversationId, List<Message> messages) {
        //message是全量的消息列表
        var redisKey = this.getKey(conversationId);
        var listOps = stringRedisTemplate.boundListOps(redisKey);

        deleteByConversationId(conversationId);

        messages.forEach(message -> listOps.rightPush(MessageUtil.toJson(message)));
    }

    @Override
    public void deleteByConversationId(@NotNull String conversationId) {
        var redisKey = this.getKey(conversationId);
        stringRedisTemplate.delete(redisKey);
    }
    private String getKey(String conversationId) {

        return this.PREFIX + conversationId;
    }
}












