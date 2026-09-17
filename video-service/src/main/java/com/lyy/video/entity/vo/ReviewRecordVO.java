package com.lyy.video.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 治理流水展示体
 */
@Data
public class ReviewRecordVO implements Serializable {

    private Long id;
    private Long adminId;
    /** 操作人姓名 */
    private String adminName;
    /** 动作中文：通过/驳回/下架/重新上架 */
    private String actionDesc;
    private String reason;
    private LocalDateTime createTime;
}
