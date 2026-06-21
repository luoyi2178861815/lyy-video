package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏夹列表项 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectFolderVO implements Serializable {

    private Long id;                // 收藏夹ID
    private String name;            // 收藏夹名称
    private String description;     // 简介
    private Integer isPublic;       // 是否公开
    private Integer videoCount;     // 收藏夹内视频数
    private String coverUrl;        // 封面图
    private LocalDateTime createTime;
}
