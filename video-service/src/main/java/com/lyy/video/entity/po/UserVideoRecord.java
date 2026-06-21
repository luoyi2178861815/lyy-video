package com.lyy.video.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户视频观看记录实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVideoRecord {
    /**
     * 记录ID
     */
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 视频ID
     */
    private Long videoId;
    /**
     * 最后观看时间点(秒)
     */
    private Integer lastWatchTime;
    /**
     * 本次观看时长(秒)
     */
    private Integer watchDuration;
    /**
     * 播放次数
     */
    private Integer playCount;
    /**
     * 是否看完：0-未看完，1-看完
     */
    private Integer isFinished;
    /**
     * 视频总时长(秒)
     */
    private Integer duration;
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}