package com.lyy.interaction.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentAddDTO {
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    @NotNull(message = "父评论ID不能为空，一级评论请传0")
    private Long parentId;         // 被回复的评论ID（0表示一级评论）

    private Long replyToUserId; // 被回复的用户ID（当parentId不为0时可选）

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容最多1000字")
    private String content;
}