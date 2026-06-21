package com.lyy.video.config;

import com.lyy.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public DirectExchange videoCommentExchange() {
        return new DirectExchange(MqConstant.VIDEO_COMMENT_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue videoCommentQueue() {
        return new Queue(MqConstant.VIDEO_COMMENT_QUEUE, true);
    }
    
    @Bean
    public Binding videoCommentBinding() {
        return BindingBuilder.bind(videoCommentQueue())
                .to(videoCommentExchange())
                .with(MqConstant.VIDEO_COMMENT_ROUTING_KEY);
    }
}
