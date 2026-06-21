package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频点赞记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoLike {
    private Long id;            // 主键
    private Long videoId;     // 视频ID
    private Long userId;        // 点赞用户ID
    private LocalDateTime createTime; // 点赞时间
}
