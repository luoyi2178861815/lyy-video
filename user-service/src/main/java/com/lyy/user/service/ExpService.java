package com.lyy.user.service;

import com.lyy.user.entity.po.ExpRecord;

import java.util.List;

public interface ExpService {

    /** 添加经验值，内部处理每日上限、首次绑定、等级升级 */
    void addExp(Long userId, int expValue, String reason);

    /** 查询用户最近经验记录 */
    List<ExpRecord> getRecentRecords(Long userId);
}
