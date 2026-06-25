package com.lyy.aigc.service;


import com.lyy.aigc.entity.dto.ChatDTO;
import com.lyy.aigc.entity.vo.ChatEventVO;
import com.lyy.common.context.BaseContext;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 获取对话id，规则：用户id_会话id
     *
     * @param sessionId 会话id
     * @return 对话id
     */
    static String getConversationId(String sessionId) {
        return BaseContext.getCurrentId() + "_" + sessionId;
    }

    Flux<ChatEventVO> chat(ChatDTO chatDTO);

    void stop(String sessionId);
}
