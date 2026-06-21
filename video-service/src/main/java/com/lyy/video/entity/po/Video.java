package com.lyy.video.entity.po;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class    Video {
    private Long id;                     // 视频ID
    private Long userId;                 // 上传用户ID
    private String videoUrl;             // 视频存储URL
    private String coverUrl;             // 封面图URL
    private String title;                // 标题
    private String introduction;         // 简介
    private Integer statementCode;       // 内容声明code
    private Integer partitionCode;       // 分区code
    private String tags;                 // 标签（逗号分隔）
    private Long collectCount;           // 收藏数
    private Integer duration;            // 视频时长（秒）
    private Long commentCount;           // 评论数
    private Long playCount;              // 播放次数
    private Integer status;              // 状态（1:已发布,2:已下架,3:审核中,4:私密）
    private Long totalWatchDuration;     // 总观看时长（秒）
    private BigDecimal hotScore;         // 热度值
    private Long likeCount;              // 点赞数
    private Long coinCount;              // 投币数
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}