package com.lyy.interaction.service;

import com.lyy.common.result.PageResult;

public interface FollowService {

    /** 关注/取关 toggle，返回 true=已关注 false=已取关 */
    boolean toggleFollow(Long userId, Long followeeId);

    /** 分页查询关注列表（含用户信息） */
    PageResult getFollowingList(Long userId, int pageNum, int pageSize);

    /** 分页查询粉丝列表（含用户信息） */
    PageResult getFansList(Long userId, int pageNum, int pageSize);

    /** 是否已关注 */
    boolean isFollowing(Long userId, Long followeeId);
}
