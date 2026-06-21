# 关注系统 API 文档

## 基础信息

- **Base URL**: `/interact/follow`
- **认证**: 所有接口需携带登录Token（通过 `BaseContext.getCurrentId()` 获取当前用户ID）

---

## 1. 关注/取关用户

**POST** `/interact/follow/{userId}/follow`

Toggle 模式：已关注则取关，未关注则关注。

**路径参数:**

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 目标用户ID |

**流程:**
1. 校验不能关注自己
2. 查询是否已关注 → 已关注则删除记录（取关），未关注则插入记录（关注）
3. 发 MQ → user-service 异步更新 `follow_count` 和 `fans_count`

**Response（关注）:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "followed": true
    }
}
```

**Response（取关）:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "followed": false
    }
}
```

**错误码:**
- `用户不存在或者未登录` — 未登录
- `目标用户ID不能为空` — 参数缺失
- `不能关注自己` — userId 与当前用户相同

---

## 2. 我的关注列表（分页）

**GET** `/interact/follow/following/list?pageNum=1&pageSize=10`

**Query Parameters:**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | int | 1 | 页码 |
| pageSize | int | 10 | 每页条数 |

**Response:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "total": 25,
        "records": [
            {
                "userId": 1002,
                "nickname": "UP主小明",
                "avatar": "https://cdn.example.com/avatar/1002.jpg",
                "followTime": "2026-06-10T15:30:00",
                "isMutual": null
            }
        ]
    }
}
```

用户信息通过 Feign 批量查 user-service，失败时昵称/头像返回 null（降级）。

---

## 3. 我的粉丝列表（分页）

**GET** `/interact/follow/fans/list?pageNum=1&pageSize=10`

**Query Parameters:**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | int | 1 | 页码 |
| pageSize | int | 10 | 每页条数 |

**Response:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "total": 100,
        "records": [
            {
                "userId": 1003,
                "nickname": "粉丝小红",
                "avatar": "https://cdn.example.com/avatar/1003.jpg",
                "followTime": "2026-06-09T10:00:00",
                "isMutual": null
            }
        ]
    }
}
```

---

## 4. 是否已关注某用户

**GET** `/interact/follow/{userId}/status`

**路径参数:**

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 目标用户ID |

**Response:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "following": true
    }
}
```

---

## 5. MQ 消息格式

### FollowMessage

交互服务 → 用户服务，用于异步更新关注数和粉丝数。

**Exchange:** `user.follow.exchange`
**Routing Key:** `user.follow.increment`
**Queue:** `user.follow.queue`

```json
{
    "followerId": 1001,
    "followeeId": 1002,
    "increment": 1
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| followerId | Long | 关注者ID |
| followeeId | Long | 被关注者ID |
| increment | Integer | +1（关注）或 -1（取关） |

用户服务消费者 `FollowConsumer.handleFollowIncrement()`:
- `UserServiceImpl.incrementFollowCount(followerId, increment)` → SQL `GREATEST(0, follow_count + #{increment})`
- `UserServiceImpl.incrementFansCount(followeeId, increment)` → SQL `GREATEST(0, fans_count + #{increment})`

---

## 6. 数据库

### user_follow（关注记录表）

```sql
CREATE TABLE user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL COMMENT '关注者',
    followee_id BIGINT NOT NULL COMMENT '被关注者',
    create_time DATETIME COMMENT '关注时间',
    UNIQUE KEY uk_follower_followee (follower_id, followee_id)
);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| follower_id | BIGINT | 关注者用户ID |
| followee_id | BIGINT | 被关注者用户ID |
| create_time | DATETIME | 关注时间 |

唯一约束: `(follower_id, followee_id)` —— 同一个用户不能重复关注同一人
