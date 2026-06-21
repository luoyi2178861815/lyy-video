package com.lyy.interaction.entity.po;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Comment {
    private Long id;                 // 评论ID
    private Long videoId;            // 所属视频ID
    private Long userId;             // 评论用户ID
    private Long parentId;           // 父评论ID（0表示一级评论）
    private Long rootId;             // 根评论ID（一级评论ID）
    private Long replyToUserId;      // 被回复的用户ID（可为空）
    private String content;          // 评论内容
    private Integer likeCount;       // 点赞数
    private Integer status;          // 状态：0-删除 1-正常 2-审核中
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
