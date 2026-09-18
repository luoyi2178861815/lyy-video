package com.lyy.video.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注动态流单条卡片数据
 * 三个计数直接来自 video 表的冗余列（like_count / comment_count / collect_count），
 * 不需要任何跨服务调用；publishTime 直接给 LocalDateTime，前端用 timeAgo 转「10分钟前」
 */
@Data
public class FeedItemVO {

    private Long videoId;

    private String title;

    private String coverUrl;

    private Integer duration;

    private Long authorId;

    /** 昵称；作者资料 Feign 失败时兜底为「用户{authorId}」 */
    private String authorName;

    /** 头像；作者资料 Feign 失败时为空串 */
    private String authorAvatar;

    private Long likeCount;

    private Long commentCount;

    private Long collectCount;

    /** = video.create_time */
    private LocalDateTime publishTime;

    /** 当前用户是否已赞 */
    private Boolean liked;

    /**
     * 点赞状态不可用（批量查点赞状态的 Feign 降级）时为 true
     * 前端据此把 ♥ 置为禁用态：likeVideo 是 toggle 语义，
     * 状态错会让用户一点反而「取消」了真实的赞，禁用比误导安全
     */
    private Boolean likeDisabled;
}
