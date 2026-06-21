package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏夹
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectFolder {
    private Long id;                // 收藏夹ID
    private Long userId;            // 所属用户ID
    private String name;            // 收藏夹名称
    private String description;     // 简介
    private Integer isPublic;       // 是否公开：0私密 1公开
    private Integer videoCount;     // 收藏夹内视频数（冗余）
    private String coverUrl;        // 封面图
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
