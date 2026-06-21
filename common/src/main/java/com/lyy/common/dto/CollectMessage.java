package com.lyy.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏数增量消息体
 */
@Data
@NoArgsConstructor
public class CollectMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long videoId;       // 视频ID
    private Integer increment;  // 增量：+1收藏 / -1取消收藏
}
