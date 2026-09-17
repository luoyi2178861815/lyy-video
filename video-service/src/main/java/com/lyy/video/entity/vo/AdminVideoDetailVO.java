package com.lyy.video.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端视频详情
 * 在列表行字段基础上补充审核时需要的完整信息
 */
@Data
public class AdminVideoDetailVO extends AdminVideoVO {

    /** 视频存储路径，前端拼 /api/video/file/{filename} 播放 */
    private String videoUrl;
    private String introduction;
    private String tags;
    private Integer statementCode;
    private Long playCount;
    private Long likeCount;
    private Long coinCount;
    private Long collectCount;
    private Long commentCount;
    private LocalDateTime updateTime;
}
