package com.lyy.user.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C端 / B端 通用的用户信息返回体（不含密码等敏感字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String sign;
    private String phone;
    private Integer status;
    private Integer level;
    private Integer exp;
    private Long followCount;
    private Long fansCount;
    private Long likeTotal;
    private Long playTotal;
    private Long coinCount;
    private String email;
    private Integer realNameVerified;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
