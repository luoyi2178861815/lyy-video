package com.lyy.user.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * B端 - 管理员编辑用户
 */
@Data
public class AdminUserUpdateDTO implements Serializable {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @Size(min = 6, max = 50, message = "密码长度6-50位")
    private String password;

    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;

    @Size(max = 255, message = "头像URL最长255字符")
    private String avatar;

    @Size(max = 255, message = "签名最长255字符")
    private String sign;

    private String phone;

    private Integer status;

    private Integer level;

    private Integer exp;

    private Long coinCount;

    private String email;

    private Integer realNameVerified;
}
