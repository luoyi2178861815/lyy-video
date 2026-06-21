package com.lyy.interaction.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Feign 调 user-service 返回的最小用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentUserVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}
