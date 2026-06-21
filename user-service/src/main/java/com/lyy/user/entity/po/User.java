package com.lyy.user.entity.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户核心实体信息")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户主键id")
    private Long id;

    @Schema(description = "登录账号（唯一）")
    private String username;

    @Schema(description = "加密后的密码", hidden = true) // 接口文档中隐藏密码敏感字段
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像url")
    private String avatar;

    @Schema(description = "个性签名")
    private String sign;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "账号状态: 1正常 0禁用")
    private Integer status;

    @Schema(description = "用户等级")
    private Integer level;

    @Schema(description = "升级经验值")
    private Integer exp;

    @Schema(description = "我关注的人数")
    private Long followCount;

    @Schema(description = "我的粉丝数量")
    private Long fansCount;

    @Schema(description = "所有作品总获赞")
    private Long likeTotal;

    @Schema(description = "所有作品总播放量")
    private Long playTotal;

    @Schema(description = "硬币数量")
    private Long coinCount;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "实名认证: 0未认证 1已认证")
    private Integer realNameVerified;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
}