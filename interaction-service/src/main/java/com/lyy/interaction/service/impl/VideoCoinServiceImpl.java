package com.lyy.interaction.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.CoinMessage;
import com.lyy.common.dto.ExpMessage;
import com.lyy.common.result.Result;
import com.lyy.interaction.config.AsyncConfig;
import com.lyy.interaction.entity.po.MqMessageLog;
import com.lyy.interaction.entity.po.VideoCoin;
import com.lyy.interaction.feign.UserFeignClient;
import com.lyy.interaction.mapper.MqMessageLogMapper;
import com.lyy.interaction.mapper.VideoCoinMapper;
import com.lyy.interaction.service.VideoCoinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoCoinServiceImpl implements VideoCoinService {

    @Autowired
    private VideoCoinMapper videoCoinMapper;

    @Autowired
    private AsyncConfig asyncConfig;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserFeignClient userFeignClient;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;


    //分布式锁key
    private static final String COIN_LOCK_KEY_PREFIX = "coin:lock:";
    private static final String COIN_CACHE_KEY_PREFIX = "coin:cache:";
    private static final long LOCK_EXPIRE_TIME = 5L;
    private static final long CACHE_EXPIRE_DAYS = 30L;
    //

    @Override
    @Transactional
    @SentinelResource("giveCoin")
    public Result<String> giveCoin(Long userId, Long videoId) {
        String lockKey = COIN_LOCK_KEY_PREFIX + userId + ":" + videoId;
        String cacheKey = COIN_CACHE_KEY_PREFIX + userId + ":" + videoId;
        
        try {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            
            if (Boolean.FALSE.equals(locked)) {
                log.warn("用户 {} 对视频 {} 的投币请求正在处理中", userId, videoId);
                return Result.error("请勿重复提交");
            }
            
            Boolean isCoined = stringRedisTemplate.hasKey(cacheKey);
            if (Boolean.TRUE.equals(isCoined)) {
                log.info("缓存命中：用户 {} 已对视频 {} 投过币", userId, videoId);
                return Result.error("您已经对该视频投过币了");
            }
            
            Result<Integer> levelResult = userFeignClient.getUserLevel(userId);
            if (levelResult != null && levelResult.getData() != null && levelResult.getData() == 0) {
                return Result.error("Lv0 用户暂不支持投币，请先获取经验升级");
            }
        
            VideoCoin coin = new VideoCoin();
            coin.setUserId(userId);
            coin.setVideoId(videoId);
            coin.setCreateTime(LocalDateTime.now());
            
            try {
                videoCoinMapper.insert(coin);
            } catch (DuplicateKeyException e) {
                log.warn("用户 {} 对视频 {} 重复投币（数据库唯一约束）", userId, videoId);
                stringRedisTemplate.opsForValue().set(cacheKey, "1", CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                return Result.error("您已经对该视频投过币了");
            }
            //异步处理投币消息
            asyncConfig.handleMqTaskExecutor().execute(() -> {
                try {
                    CoinMessage msg = new CoinMessage();
                    msg.setUserId(userId);
                    msg.setVideoId(videoId);
                    msg.setIncrement(1);
//
//                    MqMessageLog MqLog = new MqMessageLog();
//                    MqLog.setExchange(MqConstant.VIDEO_COIN_EXCHANGE);
//                    MqLog.setRoutingKey(MqConstant.VIDEO_COIN_ROUTING_KEY);
//                    MqLog.setCreateTime(LocalDateTime.now());
//                    MqLog.setUpdateTime(LocalDateTime.now());
//                    MqLog.setStatus(0);
//                    MqLog.setRetryCount(0);
//                    mqMessageLogMapper.insert(MqLog);
//                    stringRedisTemplate.opsForValue().set(cacheKey, "1", CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                    log.info("用户 {} 给视频 {} 投币成功", userId, videoId);
                    rabbitTemplate.convertAndSend(MqConstant.VIDEO_COIN_EXCHANGE, MqConstant.VIDEO_COIN_ROUTING_KEY, msg);
                    ExpMessage coinExpMsg = ExpMessage.of(userId, 10, "daily_coin");
                    rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, coinExpMsg);

                    stringRedisTemplate.opsForValue().set(cacheKey, "1", CACHE_EXPIRE_DAYS, TimeUnit.DAYS);}

                catch (Exception exception) {
                    log.error("用户 {} 给视频 {} 投币失败", userId, videoId, exception);

                }
            });
            return Result.success("投币成功，经验+10");
            
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public boolean isCoinedByUser(Long userId, Long videoId) {
        String cacheKey = COIN_CACHE_KEY_PREFIX + userId + ":" + videoId;
        Boolean exists = stringRedisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }
        
        boolean dbExists = videoCoinMapper.exists(userId, videoId) > 0;
        if (dbExists) {
            stringRedisTemplate.opsForValue().set(cacheKey, "1", CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        }
        return dbExists;
    }
}
