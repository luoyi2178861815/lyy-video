-- 用户经验值 & 等级系统 DDL
-- user 表新增字段
ALTER TABLE `user`
  ADD COLUMN `email` VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  ADD COLUMN `real_name_verified` TINYINT DEFAULT 0 COMMENT '实名认证: 0未认证 1已认证';

-- 经验记录表
CREATE TABLE IF NOT EXISTS `user_exp_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `exp_value` INT NOT NULL COMMENT '经验变化量',
    `reason` VARCHAR(50) NOT NULL COMMENT '来源: daily_login/daily_watch/daily_coin/daily_share/bind_email/bind_phone/real_name',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_time` (`user_id`, `create_time`)
) COMMENT '用户经验记录表，每个用户保留最近30条';

-- 视频表新增投币数量字段（在 db_video 库执行）
ALTER TABLE `video`
  ADD COLUMN `coin_count` BIGINT DEFAULT 0 COMMENT '投币数';

-- 投币记录表（在 db_interaction 库执行）
CREATE TABLE IF NOT EXISTS `video_coin` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `video_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_video` (`user_id`, `video_id`)
) COMMENT '用户投币记录表';
