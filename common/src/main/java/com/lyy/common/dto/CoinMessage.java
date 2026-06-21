package com.lyy.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long videoId;
    private Integer increment;
}
