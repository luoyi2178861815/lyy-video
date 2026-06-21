package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论树返回体（含分页信息）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentTreesVO {
    private Long videoId;                   // 视频ID
    private long total;                     // 根评论总数
    private List<CommentVO> commentTrees;   // 当前页评论树
}
