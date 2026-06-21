package com.lyy.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 关注/取关消息体
 */
@Data
@NoArgsConstructor
public class FollowMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long followerId;    // 关注者
    private Long followeeId;    // 被关注者
    private Integer increment;  // +1关注 / -1取关
}
