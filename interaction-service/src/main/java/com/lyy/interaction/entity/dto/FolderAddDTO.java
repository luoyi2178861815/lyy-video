package com.lyy.interaction.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建收藏夹
 */
@Data
public class FolderAddDTO implements Serializable {

    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 50, message = "收藏夹名称最长50字符")
    private String name;

    @Size(max = 255, message = "简介最长255字符")
    private String description;

    private Integer isPublic;   // 0私密 1公开，默认0
}
