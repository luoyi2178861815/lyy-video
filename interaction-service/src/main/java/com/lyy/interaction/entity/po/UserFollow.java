package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {
    private Long id;
    private Long followerId;    // 关注者
    private Long followeeId;    // 被关注者
    private LocalDateTime createTime;
}
