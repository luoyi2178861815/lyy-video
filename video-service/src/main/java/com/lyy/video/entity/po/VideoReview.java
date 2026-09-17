package com.lyy.video.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频治理流水
 */
@Data
public class VideoReview implements Serializable {

    private Long id;
    private Long videoId;
    private Long adminId;
    /** 操作人姓名，登录时从 Admin-Token 解出并随请求头传入 */
    private String adminName;
    /** 见 VideoReviewActionEnum：1通过 2驳回 3下架 4重新上架 */
    private Integer action;
    private String reason;
    private LocalDateTime createTime;
}
