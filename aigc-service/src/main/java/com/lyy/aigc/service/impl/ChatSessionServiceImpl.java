package com.lyy.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.lyy.aigc.config.SessionProperties;
import com.lyy.aigc.entity.po.ChatSession;
import com.lyy.aigc.entity.vo.MessageVO;
import com.lyy.aigc.entity.vo.SessionVO;
import com.lyy.aigc.enums.MessageTypeEnum;
import com.lyy.aigc.mapper.ChatSessionMapper;
import com.lyy.aigc.service.ChatService;
import com.lyy.aigc.service.ChatSessionService;
import com.lyy.common.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMemory chatMemory;


    public SessionVO createSession(Integer num) {
        var sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));
        Long userId = BaseContext.getCurrentId();
        // 随机生成sessionId
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());
        // 构建持久化对象，并持久化
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionVO.getSessionId());
        chatSession.setUserId(userId);
        chatSession.setCreateTime(LocalDateTime.now());
        chatSession.setUpdater(userId);
        chatSession.setCreater(userId);
        chatSessionMapper.insert(chatSession);
        return sessionVO;
    }


    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        log.info("查询会话, sessionId: {}, conversationId: {}, userId: {}", sessionId, conversationId, BaseContext.getCurrentId());
        try {
            List<Message> messages = this.chatMemory.get(conversationId);
            return StreamUtil.of(messages)
                    .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                    .map(message -> MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("chatMemory.get 异常", e);
            throw e;
        }
    }
}
