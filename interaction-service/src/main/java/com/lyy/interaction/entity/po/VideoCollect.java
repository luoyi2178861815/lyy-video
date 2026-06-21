package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频收藏记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCollect {
    private Long id;                // 主键
    private Long userId;            // 收藏用户ID
    private Long videoId;           // 视频ID
    private Long folderId;          // 所属收藏夹ID
    private LocalDateTime createTime; // 收藏时间
}
