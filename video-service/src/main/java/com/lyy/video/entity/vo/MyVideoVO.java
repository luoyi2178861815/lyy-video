package com.lyy.video.entity.vo;

import lombok.Data;

@Data
public class MyVideoVO {
    //返回视频基本信息
    private Long videoId;
    private String videoUrl;
    private String coverUrl;
    private String title;
    private Long likeCount;
    private Long playCount;
    private Long commentCount;
}
