-- ==================== 视频治理流水（库：db_video） ====================
-- 每次审核/下架/重新上架都插一条，可追溯「谁在什么时候因为什么做了什么」
CREATE TABLE IF NOT EXISTS video_review (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    video_id    BIGINT       NOT NULL COMMENT '视频ID',
    admin_id    BIGINT       NOT NULL COMMENT '操作人ID',
    admin_name  VARCHAR(32)           COMMENT '操作人姓名（跨库冗余，避免 N+1 次 Feign）',
    action      TINYINT      NOT NULL COMMENT '1通过 2驳回 3下架 4重新上架',
    reason      VARCHAR(255)          COMMENT '驳回/下架原因',
    create_time DATETIME              COMMENT '操作时间',
    KEY idx_video_id (video_id)
) COMMENT '视频治理流水';
