package com.lyy.aigc.mapper;

import com.lyy.aigc.entity.po.ChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    void insert(ChatSession chatSession);

    List<ChatSession> selectBySessionId(String sessionId, Long userId);

    void updateById(ChatSession chatSession);

    List<ChatSession> selectByUserId(Long userId);

    void deleteBySessionId(String sessionId, Long userId);
}
