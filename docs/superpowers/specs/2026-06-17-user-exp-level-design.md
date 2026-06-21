# 用户经验值 & 等级系统设计文档

日期：2026-06-17 | 状态：已确认

---

## 一、需求概述

为平台用户实现经验值获取与等级成长体系（对标 B 站 Lv0-Lv6），激励用户活跃。

## 二、业务规则

### 2.1 经验值获取规则

| # | 行为 | 经验值 | 每日上限 | 触发方式 |
|---|------|--------|----------|----------|
| 1 | 每日登录 | +5 | 1次/天 | user-service 登录接口 |
| 2 | 每日观看视频 | +5 | 1次/天 | video-service → MQ |
| 3 | 每日投币 | +10/次 | 5次/天(50) | interaction-service → MQ |
| 4 | 每日分享视频 | +5 | 1次/天 | user-service API |
| 5 | 首次绑定邮箱 | +20 | 永久1次 | user-service 绑定接口 |
| 6 | 首次绑定手机号 | +20 | 永久1次 | user-service 绑定接口 |
| 7 | 首次实名认证 | +50 | 永久1次 | user-service 认证接口 |

每日上限 0 点重置（自然日），经验值只增不减，等级只升不降。

### 2.2 等级梯度

| 等级 | 经验区间 | 称号 |
|------|----------|------|
| Lv0 | 0 | 注册游客，未转正，限制互动 |
| Lv1 | 1-199 | 正式新用户，解锁基础互动 |
| Lv2 | 200-1499 | 轻度活跃观众 |
| Lv3 | 1500-4499 | 常驻爱好者 |
| Lv4 | 4500-10799 | 深度核心用户 |
| Lv5 | 10800-28799 | 资深老用户 |
| Lv6 | ≥28800 | 满级元老 |

### 2.3 Lv0 限制

- 禁止评论
- 禁止投币
- 点赞、关注、收藏正常开放

### 2.4 硬币规则

- 每日登录并观看视频后自动获得 1 个硬币
- 硬币用于投币消耗，换取经验值

---

## 三、数据库变更

### 3.1 user 表新增字段

```sql
ALTER TABLE `user`
  ADD COLUMN `email` VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  ADD COLUMN `real_name_verified` TINYINT DEFAULT 0 COMMENT '实名认证: 0未认证 1已认证';
```

手机号绑定状态通过 `phone` 字段是否非空判断。

### 3.2 新建 user_exp_record 表

```sql
CREATE TABLE `user_exp_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `exp_value` INT NOT NULL COMMENT '经验变化量',
    `reason` VARCHAR(50) NOT NULL COMMENT '来源: daily_login/daily_watch/daily_coin/daily_share/bind_email/bind_phone/real_name',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_time` (`user_id`, `create_time`)
) COMMENT '用户经验记录表';
```

每个用户只保留最近 30 条记录，超出自动删除最旧记录。

### 3.3 现有数据修正

新用户注册默认 `level = 0, exp = 0`（现有代码默认 level=1 需修正）。

---

## 四、MQ 消息设计

### 4.1 新增交换机/队列

| 元素 | 值 |
|------|-----|
| 交换机 | `exp.direct` |
| 队列 | `exp.queue` |
| 路由键 | `exp.add` |

### 4.2 消息体 ExpMessage（common 模块）

```java
public class ExpMessage implements Serializable {
    private Long userId;
    private Integer expValue;   // 经验变化量
    private String reason;      // 来源枚举值
    private LocalDateTime timestamp;
}
```

### 4.3 生产者

| 消息来源 | 服务 | 触发点 |
|----------|------|--------|
| daily_watch | video-service | 用户首次观看视频 |
| daily_coin | interaction-service | 用户投币 |
| daily_login / daily_share / bind_* | user-service | 对应业务方法内 |

### 4.4 消费者

user-service 新增 `ExpConsumer`，监听 `exp.queue`，调用 `ExpService` 统一处理。

---

## 五、核心服务设计

### 5.1 ExpService

```
addExp(Long userId, int expValue, String reason):
  1. 查询用户当前 exp、level
  2. 每日任务检查（reason ∈ daily_*）:
     a. 查 user_exp_record 统计当天同 reason 条数
     b. 超限 → 直接返回（不抛异常）
  3. 首次绑定检查（reason ∈ bind_*）:
     a. 查 user_exp_record 是否有同 reason 历史记录
     b. 已有 → 直接返回
  4. 更新 user.exp += expValue
  5. 计算新等级: calcLevel(newExp)
  6. 如果等级变化，更新 user.level
  7. INSERT user_exp_record
  8. 清理旧记录（保留最近30条）
```

### 5.2 等级计算工具方法

```java
public static int calcLevel(int exp) {
    if (exp == 0) return 0;
    if (exp < 200) return 1;
    if (exp < 1500) return 2;
    if (exp < 4500) return 3;
    if (exp < 10800) return 4;
    if (exp < 28800) return 5;
    return 6;
}
```

### 5.3 BaseContext 注入 Lv0 检查

在 gateway 的 `JwtAuthGlobalFilter` 中将 `level` 一并放入 header，`UserContextInterceptor` 解析后存入 `BaseContext`，方便各服务判断权限。

---

## 六、Lv0 权限限制

- **interaction-service CommentController**：创建评论前检查 `level == 0` → 拒绝
- **interaction-service CoinController**（投币接口）：检查 `level == 0` → 拒绝

---

## 七、API 变更

### 7.1 新增接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/exp/share` | 每日分享上报（客户端调用） |
| GET | `/api/user/exp/records` | 查询当前用户经验记录（近30条） |

### 7.2 现有接口修改

| 接口 | 变更 |
|------|------|
| POST `/api/user/login` | 登录成功后发 daily_login MQ 消息 |
| POST `/api/user/register` | 新用户 level 默认值改为 0 |

---

## 八、现有代码修复清单

| 文件 | 修改内容 |
|------|---------|
| `User.java` | 新增 `email`、`realNameVerified` 字段 |
| `UserProfileVO.java` | 新增对应字段 |
| `UserMapper.xml` | INSERT/UPDATE 包含新字段 |
| `AdminUserUpdateDTO.java` | 新增字段（B端编辑） |
| `AdminUserAddDTO.java` | 新增字段（B端创建） |
| `UserServiceImpl.java` | toProfileVO() 映射新字段；register() 默认 level=0 |

---

## 九、待定/后续

- 硬币每日发放的具体逻辑（登录+观看各触发一次）
- 每日经验上限重置的定时任务（0 点清理缓存/无需清理，SQL 按日期过滤即可）
- 分享视频的经验获取需要客户端配合上报
