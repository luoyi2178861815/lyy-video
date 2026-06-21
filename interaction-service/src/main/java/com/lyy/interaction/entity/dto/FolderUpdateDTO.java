package com.lyy.interaction.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 编辑收藏夹
 */
@Data
public class FolderUpdateDTO implements Serializable {

    private Long id;  // 由 Controller 从路径参数注入，无需校验

    @Size(max = 50, message = "收藏夹名称最长50字符")
    private String name;

    @Size(max = 255, message = "简介最长255字符")
    private String description;

    private Integer isPublic;
}
