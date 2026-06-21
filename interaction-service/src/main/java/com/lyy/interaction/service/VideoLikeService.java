package com.lyy.interaction.service;

import com.lyy.common.result.PageResult;

public interface VideoLikeService {

    /** 点赞/取消点赞，返回 true=已点赞 false=已取消 */
    boolean likeVideo(Long videoId, Long userId);

    /** 分页查询用户点赞列表（含视频信息和UP主信息） */
    PageResult getUserLikePage(Long userId, int pageNum, int pageSize);

    Boolean isLike(Long userId, Long videoId);
}
