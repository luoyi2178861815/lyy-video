package com.lyy.aigc.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession implements Serializable {
    /**
     * 数据id
     */
    private Long id;

    /**
     * 会话id
     */
    private String sessionId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 会话标题
     */
    private String title;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 创建人
     */
    private Long creater;
    /**
     * 更新人
     */
    private Long updater;
}

