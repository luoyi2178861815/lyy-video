package com.lyy.interaction.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCoin implements Serializable {

    private Long id;
    private Long userId;
    private Long videoId;
    private LocalDateTime createTime;
}
