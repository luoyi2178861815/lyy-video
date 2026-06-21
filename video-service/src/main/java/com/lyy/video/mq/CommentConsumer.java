package com.lyy.video.mq;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.CoinMessage;
import com.lyy.common.dto.CollectMessage;
import com.lyy.common.dto.CommentIncrementMessage;
import com.lyy.common.dto.LikeIncrementMessage;
import com.lyy.video.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CommentConsumer {
    
    @Autowired
    private VideoService videoService;
    
    @RabbitListener(queues = MqConstant.VIDEO_COMMENT_QUEUE)
    public void handleCommentIncrement(CommentIncrementMessage message) {
        log.info("收到评论增量消息：videoId={}, increment={}", message.getVideoId(), message.getIncrement());
        try {
            videoService.incrementCommentCount(message.getVideoId(), message.getIncrement());
            log.info("评论增量消息处理成功：videoId={}", message.getVideoId());
        } catch (Exception e) {
            log.error("处理评论增量消息失败：videoId={}, error={}", message.getVideoId(), e.getMessage(), e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.VIDEO_LIKE_QUEUE),
            exchange = @Exchange(value = MqConstant.VIDEO_LIKE_EXCHANGE),
            key = MqConstant.VIDEO_LIKE_ROUTING_KEY
    ))
    public void handleVideoLikeIncrement(LikeIncrementMessage message) {
        log.info("收到点赞增量消息：videoId={}, increment={}", message.getVideoId(), message.getIncrement());
        try {
            videoService.incrementLikeCount(message.getVideoId(), message.getIncrement());
            log.info("点赞增量消息处理成功：videoId={}", message.getVideoId());
        } catch (Exception e) {
            log.error("处理点赞增量消息失败：videoId={}, error={}", message.getVideoId(), e.getMessage(), e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.VIDEO_COLLECT_QUEUE),
            exchange = @Exchange(value = MqConstant.VIDEO_COLLECT_EXCHANGE),
            key = MqConstant.VIDEO_COLLECT_ROUTING_KEY
    ))
    public void handleCollectIncrement(CollectMessage message) {
        log.info("收到收藏增量消息：videoId={}, increment={}", message.getVideoId(), message.getIncrement());
        try {
            videoService.incrementCollectCount(message.getVideoId(), message.getIncrement());
            log.info("收藏增量消息处理成功：videoId={}", message.getVideoId());
        } catch (Exception e) {
            log.error("处理收藏增量消息失败：videoId={}, error={}", message.getVideoId(), e.getMessage(), e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.VIDEO_COIN_QUEUE),
            exchange = @Exchange(value = MqConstant.VIDEO_COIN_EXCHANGE),
            key = MqConstant.VIDEO_COIN_ROUTING_KEY
    ))
    public void handleCoinIncrement(CoinMessage message) {
        log.info("收到投币增量消息：videoId={}, increment={}", message.getVideoId(), message.getIncrement());
        try {
            videoService.incrementCoinCount(message.getVideoId(), message.getIncrement());
            log.info("投币增量消息处理成功：videoId={}", message.getVideoId());
        } catch (Exception e) {
            log.error("处理投币增量消息失败：videoId={}, error={}", message.getVideoId(), e.getMessage(), e);
        }
    }
}
