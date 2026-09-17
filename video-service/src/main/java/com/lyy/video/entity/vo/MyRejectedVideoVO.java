package com.lyy.video.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C 端「审核区」列表行：我被驳回的视频 + 最新驳回原因
 */
@Data
public class MyRejectedVideoVO implements Serializable {

    private Long videoId;
    private String title;
    private String coverUrl;
    private Integer duration;
    /** 视频上传时间 */
    private LocalDateTime createTime;
    /** 最新一次驳回原因 */
    private String rejectReason;
    /** 驳回操作人姓名 */
    private String reviewerName;
    /** 驳回时间 */
    private LocalDateTime reviewTime;
}
