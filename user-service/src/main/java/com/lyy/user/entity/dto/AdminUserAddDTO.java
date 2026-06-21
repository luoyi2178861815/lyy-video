package com.lyy.user.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * B端 - 管理员新增用户
 */
@Data
public class AdminUserAddDTO implements Serializable {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度6-50位")
    private String password;

    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;

    @Size(max = 255, message = "头像URL最长255字符")
    private String avatar;

    @Size(max = 255, message = "签名最长255字符")
    private String sign;

    private String phone;

    private String email;
}
