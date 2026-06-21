package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 收藏夹详情 VO（含收藏夹信息 + 视频分页列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectFolderDetailVO implements Serializable {

    private Long folderId;              // 收藏夹ID
    private String folderName;          // 收藏夹名称
    private String description;         // 简介
    private Integer isPublic;           // 是否公开
    private long total;                 // 视频总数
    private List<CollectVideoVO> videos; // 当前页视频列表
}
