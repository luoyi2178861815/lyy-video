package com.lyy.user.entity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * C端 - 用户修改个人资料
 */
@Data
public class UserUpdateDTO implements Serializable {

    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;

    @Size(max = 255, message = "头像URL最长255字符")
    private String avatar;

    @Size(max = 255, message = "签名最长255字符")
    private String sign;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String email;
}
