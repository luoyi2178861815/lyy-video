-- ============================================================
-- 排行榜相关建表 DDL（执行前请确保 db_recommend 数据库已创建）
-- ============================================================

-- 1. 每周排行归档表
-- 每周日 23:55 计算完成后写入，保留历史周排行数据
CREATE TABLE IF NOT EXISTS `ranking_weekly` (
    `id`                BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `week_label`        VARCHAR(20)  NOT NULL                 COMMENT '周标签，如 2026-W27',
    `video_id`          BIGINT(20)   NOT NULL                 COMMENT '视频ID',
    `hot_score`         DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '本周热度值（增量加权）',
    `rank`              INT(11)      NOT NULL DEFAULT 0       COMMENT '本周排名（1=最高热度）',
    `play_increment`    BIGINT(20)   NOT NULL DEFAULT 0       COMMENT '本周播放增量',
    `like_increment`    BIGINT(20)   NOT NULL DEFAULT 0       COMMENT '本周点赞增量',
    `coin_increment`    BIGINT(20)   NOT NULL DEFAULT 0       COMMENT '本周投币增量',
    `collect_increment` BIGINT(20)   NOT NULL DEFAULT 0       COMMENT '本周收藏增量',
    `comment_increment` BIGINT(20)   NOT NULL DEFAULT 0       COMMENT '本周评论增量',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_week_label` (`week_label`),
    INDEX `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每周排行归档表';

-- 2. 全站排行归档表
-- 每日凌晨 2:00 计算完成后全量替换
CREATE TABLE IF NOT EXISTS `ranking_alltime` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `video_id`    BIGINT(20)   NOT NULL                 COMMENT '视频ID',
    `hot_score`   DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '累计热度值（绝对值加权）',
    `rank`        INT(11)      NOT NULL DEFAULT 0       COMMENT '排名（1=最高热度）',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_video_id` (`video_id`),
    INDEX `idx_rank` (`rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全站排行归档表';

-- 3. 周快照表
-- 每周日 23:55 先保存所有视频的当前计数，用于下一周计算增量
CREATE TABLE IF NOT EXISTS `weekly_snapshot` (
    `id`            BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `week_label`    VARCHAR(20) NOT NULL                COMMENT '周标签，如 2026-W27',
    `video_id`      BIGINT(20) NOT NULL                 COMMENT '视频ID',
    `play_count`    BIGINT(20) NOT NULL DEFAULT 0       COMMENT '快照时的播放量',
    `like_count`    BIGINT(20) NOT NULL DEFAULT 0       COMMENT '快照时的点赞数',
    `coin_count`    BIGINT(20) NOT NULL DEFAULT 0       COMMENT '快照时的投币数',
    `collect_count` BIGINT(20) NOT NULL DEFAULT 0       COMMENT '快照时的收藏数',
    `comment_count` BIGINT(20) NOT NULL DEFAULT 0       COMMENT '快照时的评论数',
    `create_time`   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
    PRIMARY KEY (`id`),
    INDEX `idx_week_label` (`week_label`),
    INDEX `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周快照表（用于计算周增量）';
