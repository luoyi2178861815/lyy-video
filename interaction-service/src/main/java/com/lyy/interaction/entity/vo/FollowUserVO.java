package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 关注/粉丝列表展示用 VO（含用户信息 + 关注时间）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserVO implements Serializable {

    private Long userId;
    private String nickname;
    private String avatar;
    private LocalDateTime followTime;   // 关注时间（关注列表用）
    private Boolean isMutual;           // 是否互相关注
}
