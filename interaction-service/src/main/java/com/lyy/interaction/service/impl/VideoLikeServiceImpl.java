package com.lyy.interaction.service.impl;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.constant.RedisKey;
import com.lyy.common.dto.LikeIncrementMessage;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.exception.VideoNotFoundException;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.interaction.entity.po.VideoLike;
import com.lyy.interaction.entity.vo.VideoLikeVO;
import com.lyy.interaction.feign.VideoFeignClient;
import com.lyy.interaction.mapper.VideoLikeMapper;
import com.lyy.interaction.service.VideoLikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoLikeServiceImpl implements VideoLikeService {

    @Autowired
    private VideoLikeMapper videoLikeMapper;
    @Autowired
    private VideoFeignClient videoFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private static final String USER_LIKE_KEY_PREFIX = "like:user:";

    /*
     * 点赞视频功能（toggle）
     */
    @Transactional
    public boolean likeVideo(Long videoId, Long userId) {
        // 1. 参数校验
        if (videoId == null || userId == null) throw new BusinessException("参数不能为空");

        // 2. 校验视频是否存在（复用你原有逻辑，优化Redis查询写法）
        String baseKey = RedisKey.VIDEO_INFO_BASE_PREFIX + videoId;
        Boolean hasVideo = redisTemplate.opsForHash().hasKey(baseKey, "video");
        if (!Boolean.TRUE.equals(hasVideo)) {
            Result<Integer> result = videoFeignClient.exists(videoId);
            if (result.getData() == 0) throw new VideoNotFoundException("视频不存在或已删除");
        }

        // -------------redis缓存
        String userLikeKey = USER_LIKE_KEY_PREFIX + userId;
        // 原子判断用户是否点赞
        Boolean isLiked = redisTemplate.opsForSet().isMember(userLikeKey, videoId.toString());
        String statKey = RedisKey.VIDEO_INFO_STAT_PREFIX + videoId;

        if (Boolean.TRUE.equals(isLiked)) {
            // 已点赞：取消点赞
            // Redis原子操作：移除点赞记录、点赞数-1
            log.info("User {}取消点赞 video {}", userId, videoId);
            redisTemplate.opsForSet().remove(userLikeKey, videoId.toString());
            redisTemplate.opsForHash().increment(statKey, "likeCount", -1);

            // DB删除点赞记录
            videoLikeMapper.deletelike(videoId, userId);
            // 发MQ同步DB计数
            sendMqMsg(userId, videoId, -1);
            return false;
        } else {
            // 未点赞：新增点赞
            // Redis原子操作：添加点赞记录、点赞数+1
            log.info("User {}点赞 video {}", userId, videoId);
            redisTemplate.opsForSet().add(userLikeKey, videoId.toString());
            redisTemplate.opsForHash().increment(statKey, "likeCount", 1);

            // DB插入点赞记录
            VideoLike videoLike = new VideoLike();
            videoLike.setVideoId(videoId);
            videoLike.setUserId(userId);
            videoLike.setCreateTime(LocalDateTime.now());
            videoLikeMapper.like(videoLike);
            // 发MQ同步DB计数
            sendMqMsg(userId, videoId, 1);
            return true;
        }
    }

    // 抽离MQ发送方法，简化代码
    private void sendMqMsg(Long userId, Long videoId, int incr) {
        LikeIncrementMessage userMsg = LikeIncrementMessage.buildUserMsg(userId, incr);
        LikeIncrementMessage videoMsg = new LikeIncrementMessage();
        videoMsg.setVideoId(videoId);
        videoMsg.setIncrement(incr);
        rabbitTemplate.convertAndSend(MqConstant.USER_LIKE_EXCHANGE, MqConstant.USER_LIKE_ROUTING_KEY, userMsg);
        rabbitTemplate.convertAndSend(MqConstant.VIDEO_LIKE_EXCHANGE, MqConstant.VIDEO_LIKE_ROUTING_KEY, videoMsg);
    }

    /*
     * 分页查询用户点赞列表，含视频信息
     */
    public PageResult getUserLikePage(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<VideoLike> likeRecords = videoLikeMapper.selectUserLikePage(userId, offset, pageSize);
        int total = videoLikeMapper.selectUserLikeCount(userId);

        if (likeRecords.isEmpty()) {
            return new PageResult(total, Collections.emptyList());
        }

        // 收集所有视频ID
        List<Long> videoIds = likeRecords.stream()
                .map(VideoLike::getVideoId)
                .distinct()
                .collect(Collectors.toList());

        // Feign 批量查视频信息
        Map<Long, Map<String, Object>> videoMap = fetchVideoMap(videoIds);

        // 组装 VO
        List<VideoLikeVO> voList = likeRecords.stream().map(like -> {
            Map<String, Object> video = videoMap.get(like.getVideoId());

            return VideoLikeVO.builder()
                    .videoId(like.getVideoId())
                    .title(video != null ? (String) video.get("title") : null)
                    .coverUrl(video != null ? (String) video.get("coverUrl") : null)
                    .duration(video != null ? (Integer) video.get("duration") : null)
                    .playCount(video != null ? toLong(video.get("playCount")) : null)
                    .likeCount(video != null ? toLong(video.get("likeCount")) : null)
                    .commentCount(video != null ? toLong(video.get("commentCount")) : null)
                    .likeTime(like.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return new PageResult(total, voList);
    }
    /**
     * Feign查询该用户的点赞状态
     */
    @Override
    public Boolean isLike(Long userId, Long videoId) {
        int exists = videoLikeMapper.exits(videoId, userId);
        return exists == 1;
    }

    /** Feign 批量查视频信息，失败时降级返回空 Map */
    private Map<Long, Map<String, Object>> fetchVideoMap(List<Long> videoIds) {
        try {
            Result<List<Map<String, Object>>> videoResult = videoFeignClient.getVideosByIds(videoIds);
            if (videoResult != null && videoResult.getData() != null) {
                return videoResult.getData().stream()
                        .collect(Collectors.toMap(
                                v -> ((Number) v.get("id")).longValue(),
                                v -> v,
                                (a, b) -> a));
            }
        } catch (Exception e) {
            // Feign 调用失败时降级，视频信息留空
        }
        return Collections.emptyMap();
    }

    /** 安全转换 Long（Integer → Long） */
    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        return 0L;
    }
}
