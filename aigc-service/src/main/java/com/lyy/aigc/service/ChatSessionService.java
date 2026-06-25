package com.lyy.aigc.service;

import com.lyy.aigc.entity.vo.MessageVO;
import com.lyy.aigc.entity.vo.SessionVO;

import java.util.List;

public interface ChatSessionService {
    SessionVO createSession(Integer num);

    List<MessageVO> queryBySessionId(String sessionId);
}
