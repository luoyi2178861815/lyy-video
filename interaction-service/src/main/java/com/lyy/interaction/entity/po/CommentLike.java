package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论点赞记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentLike {
    private Long id;            // 主键
    private Long commentId;     // 评论ID
    private Long userId;        // 点赞用户ID
    private LocalDateTime createTime; // 点赞时间
}
