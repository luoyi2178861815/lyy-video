package com.lyy.video.entity.vo;

import lombok.Data;

@Data
public class VideoInfoVO {
    //返回视频基本信息
    private Long videoId;
    private String videoUrl;
    private String coverUrl;
    private String title;
    private String introduction;
    private Integer statementCode;
    private Integer partitionCode;
    private String tags;
    private Integer duration;
    private Long likeCount;
    private Long collectCount;
    private Long commentCount;
    private Long playCount;
    //返回作者名称
    private String authorName;
    //返回用户专属信息
    private Integer watchDuration;
    private Long coinCount;
    private Integer isLiked;
    private Integer isCollected;
    private Integer isCoined;
}
