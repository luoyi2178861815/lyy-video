package com.lyy.video.feign.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Feign 调用 user-service 返回的最小用户信息
 * 只声明需要的字段，多余字段由 Jackson 自动忽略
 */
@Data
public class UserBriefVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}
