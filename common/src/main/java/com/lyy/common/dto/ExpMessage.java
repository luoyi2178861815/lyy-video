package com.lyy.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Integer expValue;
    private String reason;
    private LocalDateTime timestamp;

    public static ExpMessage of(Long userId, Integer expValue, String reason) {
        ExpMessage msg = new ExpMessage();
        msg.userId = userId;
        msg.expValue = expValue;
        msg.reason = reason;
        msg.timestamp = LocalDateTime.now();
        return msg;
    }
}
