package com.lyy.user.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpRecord implements Serializable {

    private Long id;
    private Long userId;
    private Integer expValue;
    private String reason;
    private LocalDateTime createTime;
}
