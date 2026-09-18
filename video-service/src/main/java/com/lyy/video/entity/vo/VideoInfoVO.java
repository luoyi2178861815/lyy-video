package com.lyy.video.entity.vo;

import lombok.Data;

@Data
public class VideoInfoVO {
    //返回视频基本信息
    private Long videoId;
    private String videoUrl;
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
    // 作者的用户ID。字段名刻意与 Video 实体保持一致（而不是叫 authorId），
    // 因为 changeToVideoInfoVO 靠 BeanUtils.copyProperties 拷贝，只认同名属性；
    // 前端「关注作者」「跳作者主页」都依赖这个字段。
    private Long userId;
    //返回作者名称
    private String authorName;
    //返回用户专属信息
    private Integer watchDuration;
    private Long coinCount;
    private Integer isLiked;
    private Integer isCollected;
    private Integer isCoined;
}
