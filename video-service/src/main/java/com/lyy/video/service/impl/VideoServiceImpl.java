package com.lyy.video.service.impl;

import com.lyy.common.context.BaseContext;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.exception.VideoNotFoundException;
import com.lyy.common.result.Result;
import com.lyy.video.entity.dto.VideoUploadDTO;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.VideoInfoVO;
import com.lyy.video.feign.InteractionFeignClient;
import com.lyy.video.mapper.UserVideoRecordMapper;
import com.lyy.video.mapper.VideoMapper;
import com.lyy.video.service.VideoService;
import com.lyy.video.utils.VideoUtil;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Service
@Slf4j
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UserVideoRecordMapper userVideoRecordMapper;
    @Autowired
    private InteractionFeignClient interactionFeignClient;
    
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
        Video video = videoMapper.getVideoInfo(videoId);
        video.setId(video.getId());
        if (videoId == null) {
            throw new VideoNotFoundException("视频不存在");
        }
        Long userId = BaseContext.getCurrentId();
        // 用户未登录
        if (userId == null) {
            return changeToVideoInfoVO(video);
        }
        //1.获取私密视频
        if(video.getStatus() != null && video.getStatus() == 4){
            if(userId == null || !userId.equals(video.getUserId())){
                throw new BusinessException("私密视频不存在");
            }
            return changeToVideoInfoVO(video, userId);
        }
        //2.视频正在审核中
        if(video.getStatus() != null && video.getStatus() == 3){
            throw new BusinessException("视频正在审核中");
        }
        //3.视频已下架
        if(video.getStatus() != null && video.getStatus() == 2){
            throw new BusinessException("视频已下架");
        }
        //4.正常视频获取信息
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
        //获取用户观看进度
        if (userVideoRecordMapper.getProgress(userId, video.getId()) != null) {
            Integer watchDuration = userVideoRecordMapper.getProgress(userId, video.getId());
            videoInfoVO.setWatchDuration(watchDuration);
        }
        else{
            videoInfoVO.setWatchDuration(0);
        }
        //获取用户点赞状态
        if (interactionFeignClient.isLike(video.getId(), userId).getData()) {
            videoInfoVO.setIsLiked(1);
        } else {
            videoInfoVO.setIsLiked(0);
        }
        //获取用户收藏状态
        if (interactionFeignClient.isCollect(video.getId(), userId).getData()) {
            videoInfoVO.setIsCollected(1);
        } else {
            videoInfoVO.setIsCollected(0);
        }
        //获取用户投币状态
        Result<Boolean> coinResult = interactionFeignClient.isCoined(video.getId(), userId);
        if (coinResult != null && coinResult.getData() != null && coinResult.getData()) {
            videoInfoVO.setIsCoined(1);
        } else {
            videoInfoVO.setIsCoined(0);
        }
        return videoInfoVO;
    }
}

