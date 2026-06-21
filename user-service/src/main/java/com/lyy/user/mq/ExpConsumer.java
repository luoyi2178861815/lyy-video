package com.lyy.user.mq;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.ExpMessage;
import com.lyy.user.service.ExpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExpConsumer {

    @Autowired
    private ExpService expService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.EXP_QUEUE),
            exchange = @Exchange(value = MqConstant.EXP_EXCHANGE),
            key = MqConstant.EXP_ROUTING_KEY
    ))
    public void handleExpMessage(ExpMessage message) {
        log.info("收到经验消息：userId={}, expValue={}, reason={}",
                message.getUserId(), message.getExpValue(), message.getReason());
        try {
            expService.addExp(message.getUserId(), message.getExpValue(), message.getReason());
            log.info("经验消息处理成功：userId={}", message.getUserId());
        } catch (Exception e) {
            log.error("经验消息处理失败：userId={}, error={}",
                    message.getUserId(), e.getMessage(), e);
        }
    }
}
