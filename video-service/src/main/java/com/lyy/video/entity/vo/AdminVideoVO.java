package com.lyy.video.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端视频列表行
 */
@Data
public class AdminVideoVO implements Serializable {

    private Long id;
    private String title;
    private String coverUrl;
    /** 时长（秒） */
    private Integer duration;
    private Integer partitionCode;
    /** 上传者ID */
    private Long userId;
    /** 上传者昵称，批量 Feign 获取 */
    private String authorName;
    /** 状态码：1已发布 2已下架 3审核中 4私密 5审核不通过 */
    private Integer status;
    /** 状态中文描述，由后端给出，前端不硬编码状态映射 */
    private String statusDesc;
    private LocalDateTime createTime;
}
