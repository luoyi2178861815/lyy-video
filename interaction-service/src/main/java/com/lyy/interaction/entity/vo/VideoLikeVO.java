package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户点赞列表展示用 VO（含视频信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLikeVO implements Serializable {

    // ---- 视频信息 ----
    private Long videoId;               // 视频ID
    private String title;               // 视频标题
    private String coverUrl;            // 封面图
    private Integer duration;           // 视频时长（秒）
    private Long playCount;             // 播放量
    private Long likeCount;             // 点赞数
    private Long commentCount;          // 评论数

    // ---- 点赞信息 ----
    private LocalDateTime likeTime;     // 用户点赞时间
}
