package com.lyy.video.utils;

import com.lyy.video.entity.po.UserVideoRecord;
import com.lyy.video.mapper.UserVideoRecordMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;

/**
 * 播放进度延迟持久化处理器
 * 前端持续上报进度时先写 Redis 缓存，10 秒内无新上报才真正写入数据库，避免频繁 DB 写操作
 */
@Slf4j
@Component
public class VideoRecordDelayTaskHandler {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserVideoRecordMapper userVideoRecordMapper;

    private static volatile boolean running = true;
    private static final DelayQueue<DelayTask<RecordTaskData>> delayQueue = new DelayQueue<>();
    private static final String RECORD_KEY_TEMPLATE = "video:record:%d";

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(this::handleDelayTask);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        log.debug("延迟任务处理器已停止");
    }

    /**
     * 后台线程：消费到期的延迟任务，若 Redis 中的 lastWatchTime 与任务快照一致则持久化到数据库
     */
    private void handleDelayTask() {
        while (running) {
            try {
                DelayTask<RecordTaskData> task = delayQueue.take();
                RecordTaskData taskData = task.getData();
                // 查询 Redis 中最新上报的 lastWatchTime
                Integer cachedTime = readRecordCache(taskData.getRecordId());
                if (cachedTime == null) {
                    continue;
                }
                // 与任务快照比对，不一致说明期间有新上报，丢弃本次旧任务
                if (!taskData.getLastWatchTime().equals(cachedTime)) {
                    log.debug("播放进度已更新，丢弃旧延迟任务：recordId={}", taskData.getRecordId());
                    continue;
                }
                // 一致则持久化到数据库
                UserVideoRecord record = new UserVideoRecord();
                record.setId(taskData.getRecordId());
                record.setLastWatchTime(cachedTime);
                record.setWatchDuration(taskData.getWatchDuration());
                record.setIsFinished(taskData.getIsFinished());
                record.setLastUpdateTime(LocalDateTime.now());
                userVideoRecordMapper.updateByRecordId(record);
                log.debug("播放进度持久化完成：recordId={}, lastWatchTime={}", taskData.getRecordId(), cachedTime);
            } catch (InterruptedException e) {
                log.error("处理延迟任务异常：{}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 添加播放进度延迟任务：先写 Redis 缓存，再投递 10 秒延迟任务
     */
    public void addVideoRecordTask(UserVideoRecord record) {
        writeRecordCache(record.getId(), record.getLastWatchTime());
        delayQueue.add(new DelayTask<>(new RecordTaskData(record), Duration.ofSeconds(10)));
    }

    private void writeRecordCache(Long recordId, Integer lastWatchTime) {
        try {
            String key = String.format(RECORD_KEY_TEMPLATE, recordId);
            redisTemplate.opsForValue().set(key, lastWatchTime);
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("写入 Redis 缓存异常：{}", e.getMessage());
        }
    }

    public Integer readRecordCache(Long recordId) {
        try {
            String key = String.format(RECORD_KEY_TEMPLATE, recordId);
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? (Integer) value : null;
        } catch (Exception e) {
            log.error("读取 Redis 缓存异常：{}", e.getMessage());
            return null;
        }
    }

    public void cleanRecordCache(Long recordId) {
        String key = String.format(RECORD_KEY_TEMPLATE, recordId);
        redisTemplate.delete(key);
    }

    @Data
    @NoArgsConstructor
    private static class RecordTaskData {

        private Long recordId;
        /** 上报的快照值，用于与 Redis 最新值比对判断是否还有新上报 */
        private Integer lastWatchTime;
        /** 本次观看时长增量 */
        private Integer watchDuration;
        /** 是否看完 */
        private Integer isFinished;

        public RecordTaskData(UserVideoRecord record) {
            this.recordId = record.getId();
            this.lastWatchTime = record.getLastWatchTime();
            this.watchDuration = record.getWatchDuration();
            this.isFinished = record.getIsFinished();
        }
    }
}
