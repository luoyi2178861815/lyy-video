package com.lyy.video.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WatchHistoryVO {
    private Long videoId;
    private String coverUrl;
    private String title;
    private Integer duration;
    private Integer lastWatchTime;
    private Integer isFinished;
    private LocalDateTime lastUpdateTime;
}
