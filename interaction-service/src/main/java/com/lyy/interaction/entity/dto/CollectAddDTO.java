package com.lyy.interaction.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 收藏视频
 */
@Data
public class CollectAddDTO implements Serializable {

    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    private Long folderId;  // 收藏夹ID，为空则放入默认收藏夹
}
