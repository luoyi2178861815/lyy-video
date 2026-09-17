package com.lyy.user.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理员登录返回体（不含密码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVO implements Serializable {

    private Long id;
    private String username;
    private String name;
    /** 管理端 JWT，TTL 2 小时 */
    private String token;
}
