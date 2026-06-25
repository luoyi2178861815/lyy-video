package com.lyy.user.service.impl;

import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.entity.po.User;
import com.lyy.user.mapper.ExpRecordMapper;
import com.lyy.user.mapper.UserMapper;
import com.lyy.user.service.ExpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ExpServiceImpl implements ExpService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExpRecordMapper expRecordMapper;

    /** 每日任务上限配置 */
    private static final int DAILY_LOGIN_MAX = 1;
    private static final int DAILY_WATCH_MAX = 1;
    private static final int DAILY_COIN_MAX = 5;
    private static final int DAILY_SHARE_MAX = 1;

    @Override
    @Transactional
    public void addExp(Long userId, int expValue, String reason) {
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("经验发放失败，用户不存在：userId={}", userId);
            return;
        }

        // 2. 每日任务上限检查
        if (reason.startsWith("daily_")) {
            int todayCount = expRecordMapper.countTodayByReason(userId, reason);
            int max = getDailyMax(reason);
            if (todayCount >= max) {
                log.info("每日上限已达，userId={}, reason={}, todayCount={}, max={}",
                        userId, reason, todayCount, max);
                return;
            }
        }

        // 3. 首次绑定检查（永久一次）
        if (reason.startsWith("bind_") || reason.equals("real_name")) {
            int totalCount = expRecordMapper.countTotalByReason(userId, reason);
            if (totalCount > 0) {
                log.info("首次绑定已领取过，userId={}, reason={}", userId, reason);
                return;
            }
        }

        // 4. 更新经验值
        userMapper.incrementExp(userId, expValue);
        int newExp = user.getExp() + expValue;

        // 5. 检查升级
        int newLevel = calcLevel(newExp);
        if (newLevel > user.getLevel()) {
            User update = new User();
            update.setId(userId);
            update.setLevel(newLevel);
            userMapper.updateById(update);
            log.info("用户升级：userId={}, oldLevel={}, newLevel={}, newExp={}",
                    userId, user.getLevel(), newLevel, newExp);
        }

        // 6. 记录经验流水
        ExpRecord record = new ExpRecord();
        record.setUserId(userId);
        record.setExpValue(expValue);
        record.setReason(reason);
        expRecordMapper.insert(record);


        log.info("经验发放成功：userId={}, expValue={}, reason={}, totalExp={}",
                userId, expValue, reason, newExp);
    }

    @Override
    public List<ExpRecord> getRecentRecords(Long userId) {

        return expRecordMapper.selectByUserId(userId, 30);
    }

    private int getDailyMax(String reason) {
        return switch (reason) {
            case "daily_login" -> DAILY_LOGIN_MAX;
            case "daily_watch" -> DAILY_WATCH_MAX;
            case "daily_coin" -> DAILY_COIN_MAX;
            case "daily_share" -> DAILY_SHARE_MAX;
            default -> 1;
        };
    }

    /** 根据经验值计算等级 */
    public static int calcLevel(int exp) {
        if (exp == 0) return 0;
        if (exp < 200) return 1;
        if (exp < 1500) return 2;
        if (exp < 4500) return 3;
        if (exp < 10800) return 4;
        if (exp < 28800) return 5;
        return 6;
    }

}
