package com.lyy.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class CommentIncrementMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long videoId;
    private Integer increment;

    // 手动添加构造函数作为 Lombok 的备用方案
    public CommentIncrementMessage(Long videoId, Integer increment) {
        this.videoId = videoId;
        this.increment = increment;
    }
}
