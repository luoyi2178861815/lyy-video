package com.lyy.video.service.impl;

import com.lyy.common.constant.RedisKey;
import com.lyy.common.context.BaseContext;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.exception.VideoNotFoundException;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.video.entity.dto.VideoUploadDTO;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.MyVideoVO;
import com.lyy.video.entity.vo.VideoInfoVO;
import com.lyy.video.feign.InteractionFeignClient;
import com.lyy.video.feign.UserFeignClient;
import com.lyy.video.mapper.UserVideoRecordMapper;
import com.lyy.video.mapper.VideoMapper;
import com.lyy.video.service.VideoService;
import com.lyy.video.utils.VideoUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.alibaba.fastjson2.JSONValidator.Type.Array;


@Service
@Slf4j
@AllArgsConstructor
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UserVideoRecordMapper userVideoRecordMapper;
    @Autowired
    private InteractionFeignClient interactionFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    private RedisTemplate<Object, Object> redisTemplate;

    // key 前缀统一引用已有的 RedisKey，管理端治理动作要用同一个值删缓存，
    // 两处各写一份字面量一旦不同步会导致缓存静默失效
    private static final String VIDEO_INFO_BASE_PREFIX = RedisKey.VIDEO_INFO_BASE_PREFIX;
    private static final String VIDEO_INFO_STAT_PREFIX = RedisKey.VIDEO_INFO_STAT_PREFIX;

    @Override
    public void uploadVideo(VideoUploadDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Video video = new Video();
        video.setUserId(userId);
        video.setVideoUrl(dto.getVideoUrl());
        video.setCoverUrl(dto.getCoverUrl());
        video.setTitle(dto.getTitle());
        video.setIntroduction(dto.getIntroduction());
        video.setStatementCode(dto.getStatementCode() != null ? dto.getStatementCode() : 0);
        video.setPartitionCode(dto.getPartitionCode() != null ? dto.getPartitionCode() : 99);
        video.setTags(dto.getTags() != null ? String.join(",", dto.getTags()) : null);
        
        long duration = VideoUtil.getDurationSeconds(dto.getVideoUrl());
        video.setDuration((int) duration);
        video.setTotalWatchDuration(0L);
        video.setHotScore(new BigDecimal("0.0"));
        video.setLikeCount(0L);
        video.setCoinCount(0L);
        video.setCollectCount(0L);
        video.setCommentCount(0L);
        video.setPlayCount(0L);
        video.setStatus(3);
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());

        videoMapper.insert(video);
        log.info("视频上传成功，userId: {}, 时长: {}秒", userId, duration);
    }

    /**
     * 获取视频信息和用户专属信息
     */
    @Transactional
    public VideoInfoVO getVideoInfo(Long videoId) {
        // 从Redis缓存中获取视频信息,基本信息和互动信息分开，baseInfo做兜底，statInfo补充
        if (videoId == null) {
        throw new VideoNotFoundException("视频不存在");
        }
        Video video;
        Map<Object, Object> videoBaseInfo = redisTemplate.opsForHash().entries(VIDEO_INFO_BASE_PREFIX + videoId);
        Map<Object, Object> videoStatInfo = redisTemplate.opsForHash().entries(VIDEO_INFO_STAT_PREFIX + videoId);

        if (videoBaseInfo.isEmpty()) {
            video = videoMapper.getVideoInfo(videoId);
            if (video == null) {
                throw new VideoNotFoundException("视频不存在");
            }
            // 只有已发布的视频才写缓存：未过审/已下架/私密的视频一旦进缓存，
            // interaction-service 的点赞校验（VideoLikeServiceImpl.likeVideo）会因
            // 缓存命中而跳过 exists 检查，导致可以对用户看不见的视频点赞
            if (video.getStatus() != null && video.getStatus() == 1) {
                redisTemplate.opsForHash().put(VIDEO_INFO_BASE_PREFIX + videoId, "video", video);
                redisTemplate.expire(VIDEO_INFO_BASE_PREFIX + videoId, 7, TimeUnit.DAYS);
            }
        } else {
            video = (Video) videoBaseInfo.get("video");
        }
        if (!videoStatInfo.isEmpty()){
            Number playCount = (Number) videoStatInfo.get("playCount");
            if (playCount != null) video.setPlayCount(playCount.longValue());
            Number likeCount = (Number) videoStatInfo.get("likeCount");
            if (likeCount != null) video.setLikeCount(likeCount.longValue());
            Number coinCount = (Number) videoStatInfo.get("coinCount");
            if (coinCount != null) video.setCoinCount(coinCount.longValue());
            Number collectCount = (Number) videoStatInfo.get("collectCount");
            if (collectCount != null) video.setCollectCount(collectCount.longValue());
            Number commentCount = (Number) videoStatInfo.get("commentCount");
            if (commentCount != null) video.setCommentCount(commentCount.longValue());
        }
        Long userId = BaseContext.getCurrentId();
            // 用户未登录
            if (userId == null) {
                return changeToVideoInfoVO(video);
            }
            //1.获取私密视频
            if (video.getStatus() != null && video.getStatus() == 4) {
                if (!userId.equals(video.getUserId())) {
                    throw new BusinessException("私密视频不存在");
                }
                return changeToVideoInfoVO(video, userId);
            }
            //2.视频正在审核中
            if (video.getStatus() != null && video.getStatus() == 3) {
                throw new BusinessException("视频正在审核中");
            }
            //3.视频已下架
            if (video.getStatus() != null && video.getStatus() == 2) {
                throw new BusinessException("视频已下架");
            }
            //4.审核不通过：仅作者本人可见
            if (video.getStatus() != null && video.getStatus() == 5
                    && !userId.equals(video.getUserId())) {
                throw new BusinessException("视频不存在");
            }

        //5.正常视频获取信息
        return changeToVideoInfoVO(video, userId);
    }
    /*
    * 更新视频的评论数
    * */
    public void incrementCommentCount(Long videoId, Integer increment) {
        log.info("更新视频 {} 的评论数，增量为 {}", videoId, increment);
        videoMapper.updateCommentCount(videoId, increment);
        log.info("视频 {} 的评论数更新成功", videoId);
    }
/*
* 更新视频的点赞数
* */
    @Override
    public void incrementLikeCount(Long videoId, Integer increment) {
        log.info("更新视频 {} 的点赞数，增量为 {}", videoId, increment);
        videoMapper.updateLikeCount(videoId, increment);
        log.info("视频 {} 的点赞数更新成功", videoId);
    }

    @Override
    public void incrementCollectCount(Long videoId, Integer increment) {
        log.info("更新视频 {} 的收藏数，增量为 {}", videoId, increment);
        videoMapper.updateCollectCount(videoId, increment);
        log.info("视频 {} 的收藏数更新成功", videoId);
    }

    @Override
    public void incrementCoinCount(Long videoId, Integer increment) {
        log.info("更新视频 {} 的投币数，增量为 {}", videoId, increment);
        videoMapper.updateCoinCount(videoId, increment);
        log.info("视频 {} 的投币数更新成功", videoId);
    }

    @Override
    public Integer exists(Long videoId) {
        log.info("查询视频 {} 是否存在", videoId);
        Integer count = videoMapper.exists(videoId);
        return count != null && count > 0 ? 1 : 0;
    }

    @Override
    public PageResult searchVideos(String keyword, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Video> list = videoMapper.searchVideos(keyword, offset, pageSize);
        long total = videoMapper.countSearchVideos(keyword);
        return new PageResult(total, list);
    }

    @Override
    public PageResult pageVideos(Integer partitionCode,  int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Video> list = videoMapper.pageVideos(partitionCode, offset, pageSize);
        long total = videoMapper.countPageVideos(partitionCode);
        return new PageResult(total, list);
    }

    // 获取用户发布的视频
    @Override
    public List<MyVideoVO> getMyVideos(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Long userId = BaseContext.getCurrentId();
        List<Video> myVideos = videoMapper.getMyVideos(offset, pageSize, userId);
        return myVideos.stream().map(video -> {
            MyVideoVO myVideoVO = new MyVideoVO();
            myVideoVO.setVideoId(video.getId());
            myVideoVO.setVideoUrl(video.getVideoUrl());
            myVideoVO.setCoverUrl(video.getCoverUrl());
            myVideoVO.setTitle(video.getTitle());
            myVideoVO.setLikeCount(video.getLikeCount());
            myVideoVO.setPlayCount(video.getPlayCount());
            myVideoVO.setCommentCount(video.getCommentCount());
            return myVideoVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Video> getVideosByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return videoMapper.selectByIds(ids);
    }
    public VideoInfoVO changeToVideoInfoVO(Video video){
        VideoInfoVO videoInfoVO = new VideoInfoVO();
        videoInfoVO.setVideoId(video.getId());
        BeanUtils.copyProperties(video, videoInfoVO);
        videoInfoVO.setWatchDuration(0);
        videoInfoVO.setIsLiked(0);
        videoInfoVO.setIsCollected(0);
        videoInfoVO.setIsCoined(0);
        return videoInfoVO;
    }

    public VideoInfoVO changeToVideoInfoVO(Video video, Long userId){
        VideoInfoVO videoInfoVO = new VideoInfoVO();
        videoInfoVO.setVideoId(video.getId());
        BeanUtils.copyProperties(video, videoInfoVO);

        //并行化获取视频信息

        CompletableFuture<String> authorNameFuture = CompletableFuture.supplyAsync(() ->
                userFeignClient.getUserByUsername(video.getUserId()).getData()
        );

        CompletableFuture<Integer> watchDurationFuture = CompletableFuture.supplyAsync(() -> {
            Integer progress = userVideoRecordMapper.getProgress(userId, video.getId());
            return progress != null ? progress : 0;
        });

        CompletableFuture<Boolean> isLikeFuture = CompletableFuture.supplyAsync(() ->
                interactionFeignClient.isLike(video.getId(), userId).getData()
        );

        CompletableFuture<Boolean> isCollectFuture = CompletableFuture.supplyAsync(() ->
                interactionFeignClient.isCollect(video.getId(), userId).getData()
        );

        CompletableFuture<Boolean> isCoinedFuture = CompletableFuture.supplyAsync(() -> {
            Result<Boolean> coinResult = interactionFeignClient.isCoined(video.getId(), userId);
            return coinResult != null && coinResult.getData() != null && coinResult.getData();
        });

        CompletableFuture.allOf(authorNameFuture, watchDurationFuture, isLikeFuture, isCollectFuture, isCoinedFuture).join();

        videoInfoVO.setAuthorName(authorNameFuture.join());
        videoInfoVO.setWatchDuration(watchDurationFuture.join());
        videoInfoVO.setIsLiked(isLikeFuture.join() ? 1 : 0);
        videoInfoVO.setIsCollected(isCollectFuture.join() ? 1 : 0);
        videoInfoVO.setIsCoined(isCoinedFuture.join() ? 1 : 0);

        return videoInfoVO;
    }
}

