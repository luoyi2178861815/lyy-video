package com.lyy.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.lyy.aigc.config.SessionProperties;
import com.lyy.aigc.entity.po.ChatSession;
import com.lyy.aigc.entity.vo.ChatSessionVO;
import com.lyy.aigc.entity.vo.MessageVO;
import com.lyy.aigc.entity.vo.SessionVO;
import com.lyy.aigc.enums.MessageTypeEnum;
import com.lyy.aigc.mapper.ChatSessionMapper;
import com.lyy.aigc.memory.MyAssistantMessage;
import com.lyy.aigc.service.ChatService;
import com.lyy.aigc.service.ChatSessionService;
import com.lyy.common.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
        String sessionId = IdUtil.fastSimpleUUID();
        sessionVO.setId(sessionId);
        // 构建持久化对象，并持久化
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setUserId(userId);
        chatSession.setUpdateTime(LocalDateTime.now());
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
                    .map(message -> {
                        if (message instanceof MyAssistantMessage myAssistantMessage) {
                            return MessageVO.builder()
                                    .content(message.getText())
                                    .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                    .params(myAssistantMessage.getParams())
                                    .build();
                        }
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.error("chatMemory.get 异常", e);
            throw e;
        }
    }

    /**
     * 更新会话更新时间
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    @Async
    public void update(String sessionId, String title, Long userId) {
        log.info("异步更新会话: sessionId={}, title={}, userId={}", sessionId, title, userId);
        var chatSessionList = chatSessionMapper.selectBySessionId(sessionId, userId);
        if (chatSessionList == null || chatSessionList.isEmpty()){
            log.warn("未找到会话: sessionId={}, userId={}", sessionId, userId);
            return;
        }
        var chatSession = chatSessionList.get(0);
        chatSession.setUpdateTime(LocalDateTime.now());
        if (chatSession.getTitle() == null && title != null) {
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
        }
        chatSessionMapper.updateById(chatSession);
        log.info("会话更新完成: id={}, title={}", chatSession.getId(), chatSession.getTitle());
    }

    /**
     * 查询历史会话列表
     */
    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        Long userId = BaseContext.getCurrentId();
        var chatSessionList = chatSessionMapper.selectByUserId(userId);
        if (CollUtil.isEmpty(chatSessionList)) {
            log.info("No chat sessions found for user: {}", userId);
            return Map.of();
        }
        List<ChatSessionVO> listVO = CollStreamUtil.toList(chatSessionList, chatSession -> ChatSessionVO.builder()
                .sessionId(chatSession.getSessionId())
                .title(chatSession.getTitle())
                .updateTime(chatSession.getUpdateTime())
                .build());
        final var TODAY = "当天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";
        LocalDateTime now = LocalDateTime.now();
        return CollStreamUtil.groupByKey(listVO, chatSessionVO -> {
            long l = Math.abs(ChronoUnit.DAYS.between(chatSessionVO.getUpdateTime().toLocalDate(), now.toLocalDate()));
            if (l == 0) {
                return TODAY;
            }
            if (l <= 30) {
                return LAST_30_DAYS;
            }
            if (l <= 365) {
                return LAST_YEAR;
            }
            return MORE_THAN_YEAR;
        });
    }

    /**
     * 删除会话
     */
    @Override
    public void deleteSession(String sessionId) {
        Long userId = BaseContext.getCurrentId();
        chatSessionMapper.deleteBySessionId(sessionId, userId);
        this.chatMemory.clear(ChatService.getConversationId(sessionId));
    }
}
