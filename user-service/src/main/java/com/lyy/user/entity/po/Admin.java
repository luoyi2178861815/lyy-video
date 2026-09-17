package com.lyy.user.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台管理员
 * 与 C 端 User 完全独立的身份体系，不复用 user 表加 role 字段
 */
@Data
public class Admin implements Serializable {

    private Long id;
    private String username;
    /** BCrypt 密文，绝不返回给前端 */
    private String password;
    private String name;
    /** 1启用 0禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
