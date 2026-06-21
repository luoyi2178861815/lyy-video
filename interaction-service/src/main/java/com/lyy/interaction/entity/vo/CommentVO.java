package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论展示用 VO（含用户信息 + 子回复）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO implements Serializable {

    private Long id;                    // 评论ID
    private Long videoId;               // 所属视频ID
    private Long userId;                // 评论用户ID
    private String username;            // 评论用户名
    private String nickname;            // 评论用户昵称
    private String avatar;              // 评论用户头像
    private Long parentId;              // 父评论ID
    private Long rootId;                // 根评论ID
    private Long replyToUserId;         // 被回复的用户ID
    private String replyToUsername;     // 被回复的用户名
    private String replyToNickname;     // 被回复的用户昵称
    private String content;             // 评论内容
    private Integer likeCount;          // 点赞数
    private Boolean liked;              // 当前用户是否已点赞
    private Integer status;             // 状态
    private LocalDateTime createTime;   // 创建时间
    private List<CommentVO> children;   // 子回复列表
}
