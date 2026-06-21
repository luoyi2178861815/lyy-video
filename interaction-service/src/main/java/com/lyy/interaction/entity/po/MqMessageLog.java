package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqMessageLog {
    private Long id;
    private String messageId;
    private String exchange;
    private String routingKey;
    private String messageBody;
    private Integer status;  // 0-待发送, 1-已发送, 2-失败
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
