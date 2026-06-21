package com.lyy.user.mq;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.FollowMessage;
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
public class FollowConsumer {

    @Autowired
    private UserService userService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.USER_FOLLOW_QUEUE),
            exchange = @Exchange(value = MqConstant.USER_FOLLOW_EXCHANGE),
            key = MqConstant.USER_FOLLOW_ROUTING_KEY
    ))
    public void handleFollowIncrement(FollowMessage message) {
        log.info("收到关注增量消息：followerId={}, followeeId={}, increment={}",
                message.getFollowerId(), message.getFolloweeId(), message.getIncrement());
        try {
            userService.incrementFollowCount(message.getFollowerId(), message.getIncrement());
            userService.incrementFansCount(message.getFolloweeId(), message.getIncrement());
            log.info("关注增量消息处理成功：followerId={}, followeeId={}",
                    message.getFollowerId(), message.getFolloweeId());
        } catch (Exception e) {
            log.error("处理关注增量消息失败：followerId={}, followeeId={}, error={}",
                    message.getFollowerId(), message.getFolloweeId(), e.getMessage(), e);
        }
    }
}
