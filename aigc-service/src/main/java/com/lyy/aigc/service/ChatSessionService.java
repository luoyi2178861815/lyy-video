package com.lyy.aigc.service;

import com.lyy.aigc.entity.vo.ChatSessionVO;
import com.lyy.aigc.entity.vo.MessageVO;
import com.lyy.aigc.entity.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService {
    SessionVO createSession(Integer num);

    List<MessageVO> queryBySessionId(String sessionId);
    /**
     * 更新会话更新时间
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    void update(String sessionId, String title, Long userId);

    /**
     * 查询历史会话列表
     */
    Map<String, List<ChatSessionVO>> queryHistorySession();

    void deleteSession(String sessionId);
}
