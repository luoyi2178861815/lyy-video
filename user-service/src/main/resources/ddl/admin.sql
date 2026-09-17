-- ==================== 管理员表（库：db_user） ====================
-- 独立于 user 表：管理员封禁/删除用户时不会波及自身账号
CREATE TABLE IF NOT EXISTS admin (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(64)  NOT NULL COMMENT 'BCrypt 密文（固定 60 字符）',
    name        VARCHAR(32)           COMMENT '姓名，用于审核流水展示',
    status      TINYINT DEFAULT 1     COMMENT '1启用 0禁用',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) COMMENT '后台管理员';

-- 注意：这里不写 INSERT 种子数据。
-- BCrypt 密文必须在运行期由 BCryptPasswordEncoder 生成（手写的密文无法验证真伪，
-- 一旦写错就是「账号密码正确却登不进去」的难查故障）。
-- 默认超管由 user-service 的 AdminSeeder 在首次启动时写入，见 Task 6。
