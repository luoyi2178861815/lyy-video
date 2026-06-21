package com.lyy.user.mq;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.CommentIncrementMessage;
import com.lyy.common.dto.LikeIncrementMessage;
import com.lyy.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VideoLikeConsumer {
    @Autowired
    private UserService userService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.USER_LIKE_QUEUE),
            exchange = @Exchange(value = MqConstant.USER_LIKE_EXCHANGE),
            key = MqConstant.USER_LIKE_ROUTING_KEY
    ))
    public void handleVideoLikeIncrement(LikeIncrementMessage message) {
        log.info("收到给视频点赞增量消息：videoId={}, increment={},userid = {}", message.getVideoId(), message.getIncrement(), message.getUserId());
        try {
            userService.incrementVideoLikeCount(message.getVideoId(), message.getIncrement(), message.getUserId());
            log.info("点赞增量消息处理成功：videoId={}", message.getVideoId());
        } catch (Exception e) {
            log.error("处理点赞增量消息失败：videoId={}, error={}", message.getVideoId(), e.getMessage(), e);
        }
    }
}
