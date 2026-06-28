package com.lyy.video.service.impl;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.context.BaseContext;
import com.lyy.common.dto.ExpMessage;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.exception.VideoNotFoundException;
import com.lyy.video.entity.po.UserVideoRecord;
import com.lyy.video.entity.po.Video;
import com.lyy.video.mapper.UserVideoRecordMapper;
import com.lyy.video.mapper.VideoMapper;
import com.lyy.video.service.UserVideoRecordService;
import com.lyy.video.utils.VideoRecordDelayTaskHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserVideoRecordServiceImpl implements UserVideoRecordService {

    @Autowired
    private UserVideoRecordMapper userVideoRecordMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoRecordDelayTaskHandler delayTaskHandler;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Integer getProgress(Long videoId) {
        if (videoId == null || videoMapper.exists(videoId) == 0) {
            throw new VideoNotFoundException("视频不存在");
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return 0;
        }
        Integer progress = userVideoRecordMapper.getProgress(userId, videoId);
        if (progress == null) {
            return 0;
        }
        return progress;
    }

    //用户点击视频开始播放
    @Transactional
    public void playVideo(Long videoId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return;
        }
        // 每日观看经验（每天首次播放即触发）
        //判断用户是否今天看过视频
        UserVideoRecord existingRecord = userVideoRecordMapper.getRecord(userId, videoId);
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);


        if (existingRecord == null) {
            Video video = videoMapper.getVideoInfo(videoId);
            UserVideoRecord record = new UserVideoRecord();
            record.setUserId(userId);
            record.setVideoId(videoId);
            record.setLastWatchTime(0);
            record.setWatchDuration(0);
            record.setPlayCount(1);
            record.setIsFinished(0);
            record.setDuration(video.getDuration());
            record.setLastUpdateTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            userVideoRecordMapper.insertUserVideoRecord(record);
            ExpMessage watchExpMsg = ExpMessage.of(userId, 5, "daily_watch");
            rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, watchExpMsg);
            log.info("发送每日观看经验消息：userId={}", userId);
        }
        if (existingRecord != null && existingRecord.getLastUpdateTime().isBefore(today)) {
            ExpMessage watchExpMsg = ExpMessage.of(userId, 5, "daily_watch");
            rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, watchExpMsg);
            log.info("发送每日观看经验消息：userId={}", userId);
        }
        if (videoId == null || videoMapper.exists(videoId) == 0) {
            throw new VideoNotFoundException("视频不存在");
        }
    }

    @Override
    @Transactional
    public void submitProgress(Long videoId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (videoId == null || videoMapper.exists(videoId) == 0) {
            throw new VideoNotFoundException("视频不存在");
        }
        // 查询已有观看记录
        UserVideoRecord record = userVideoRecordMapper.getRecord(userId, videoId);
        if (record == null) {
            // 容错：如果用户直接上报进度而没有先调用播放接口，则自动创建记录
            Video video = videoMapper.getVideoInfo(videoId);
            record = new UserVideoRecord();
            record.setUserId(userId);
            record.setVideoId(videoId);
            record.setLastWatchTime(0);
            record.setWatchDuration(0);
            record.setPlayCount(1);
            record.setIsFinished(0);
            record.setDuration(video.getDuration());
            record.setLastUpdateTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            userVideoRecordMapper.insertUserVideoRecord(record);
        }
        // Redis 缓存优先（高频上报时 DB 可能还没落库），DB 兜底
        Integer cachedTime = delayTaskHandler.readRecordCache(record.getId());

        Integer oldWatchTime = cachedTime != null ? cachedTime : record.getLastWatchTime();
        if (oldWatchTime == null) {
            oldWatchTime = 0;
        }
        if (cachedTime == null) {
            log.error("使用db兜底数据");
        }
        int newWatchTime = oldWatchTime + 5;
        record.setLastWatchTime(newWatchTime);
        record.setWatchDuration(record.getWatchDuration() != null ? record.getWatchDuration() + 5 : 5);
        record.setLastUpdateTime(LocalDateTime.now());
        // 判断是否看完
        if (record.getDuration() != null && newWatchTime >= record.getDuration() * 0.9) {
            record.setIsFinished(1);
            videoMapper.updatePlayCount(videoId);
        }
        // 走 Redis 延迟持久化
        delayTaskHandler.addVideoRecordTask(record);
    }
}