# 收藏系统 API 文档

## 基础信息

- **Base URL**: `/interact/collect`
- **认证**: 所有接口需携带登录Token（通过 `BaseContext.getCurrentId()` 获取当前用户ID）

---

## 1. 收藏夹 CRUD

### 1.1 创建收藏夹

**POST** `/interact/collect/folder`

**Request Body:**
```json
{
    "name": "学习资料",
    "description": "前端学习视频合集",
    "isPublic": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 收藏夹名称 |
| description | String | 否 | 描述 |
| isPublic | Integer | 否 | 是否公开，0-私有 1-公开，默认0 |

**Response:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "id": 1,
        "name": "学习资料",
        "description": "前端学习视频合集",
        "isPublic": 1,
        "videoCount": 0,
        "coverUrl": "",
        "createTime": "2026-06-10T12:00:00"
    }
}
```

---

### 1.2 删除收藏夹

**DELETE** `/interact/collect/folder/{id}`

删除收藏夹及其下所有收藏记录，同时通过MQ扣减对应视频的收藏数。

**Response:**
```json
{
    "code": 1,
    "msg": "success"
}
```

**错误码:**
- `收藏夹不存在` — folderId 无效
- `只能删除自己的收藏夹` — 越权操作

---

### 1.3 编辑收藏夹

**PUT** `/interact/collect/folder/{id}`

**Request Body:**
```json
{
    "name": "前端进阶",
    "description": "更新后的描述",
    "isPublic": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 收藏夹名称 |
| description | String | 否 | 描述 |
| isPublic | Integer | 否 | 是否公开 |

**Response:**
```json
{
    "code": 1,
    "msg": "success"
}
```

---

### 1.4 我的收藏夹列表

**GET** `/interact/collect/folder/list`

首次调用时自动创建"默认收藏夹"（懒加载）。

**Response:**
```json
{
    "code": 1,
    "msg": "success",
    "data": [
        {
            "id": 1,
            "name": "默认收藏夹",
            "description": "",
            "isPublic": 0,
            "videoCount": 3,
            "coverUrl": "",
            "createTime": "2026-06-10T12:00:00"
        },
        {
            "id": 2,
            "name": "学习资料",
            "description": "前端学习视频合集",
            "isPublic": 1,
            "videoCount": 5,
            "coverUrl": "",
            "createTime": "2026-06-10T13:00:00"
        }
    ]
}
```

---

### 1.5 查看收藏夹详情（分页）

**GET** `/interact/collect/folder/{id}?pageNum=1&pageSize=10`

**Query Parameters:**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**Response:**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "folderId": 2,
        "folderName": "学习资料",
        "description": "前端学习视频合集",
        "isPublic": 1,
        "total": 25,
        "videos": [
            {
                "videoId": 1001,
                "title": "Spring Boot 入门教程",
                "coverUrl": "https://cdn.example.com/cover/1001.jpg",
                "duration": 1200,
                "playCount": 15000,
                "likeCount": 320,
                "commentCount": 56,
                "collectTime": "2026-06-10T14:30:00"
            }
        ]
    }
}
```

视频信息通过 Feign 批量查询 video-service，失败时视频字段返回 null（降级处理）。

---

## 2. 收藏 / 取消收藏

### 2.1 收藏视频

**POST** `/interact/collect`

**Request Body:**
```json
{
    "videoId": 1001,
    "folderId": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| videoId | Long | 是 | 视频ID |
| folderId | Long | 否 | 收藏夹ID，为空则放入默认收藏夹 |

**流程:**
1. Feign 调 video-service 验证视频存在
2. 检查是否已收藏（防重）
3. 确定目标收藏夹（指定 → 校验归属；未指定 → 默认收藏夹）
4. 插入收藏记录
5. 收藏夹 video_count +1
6. 发 MQ 消息，video-service 异步更新视频的 collect_count

**Response:**
```json
{
    "code": 1,
    "msg": "success"
}
```

**错误码:**
- `视频不存在或已删除` — videoId 无效
- `已收藏过该视频` — 重复收藏
- `收藏夹不存在` — folderId 无效或不属于当前用户

---

### 2.2 取消收藏

**DELETE** `/interact/collect/{videoId}`

**流程:**
1. 查询收藏记录（获取所属收藏夹ID）
2. 删除收藏记录
3. 收藏夹 video_count -1
4. 发 MQ 消息，video-service 异步扣减视频的 collect_count

**Response:**
```json
{
    "code": 1,
    "msg": "success"
}
```

**错误码:**
- `未收藏该视频` — 未收藏过此视频

---

## 3. MQ 消息格式

### CollectMessage

交互服务 → 视频服务，用于异步更新视频收藏数。

**Exchange:** `video.collect.exchange`
**Routing Key:** `video.collect.increment`
**Queue:** `video.collect.queue`

```json
{
    "videoId": 1001,
    "increment": 1
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | Long | 视频ID |
| increment | Integer | +1（收藏）或 -1（取消收藏/删除收藏夹） |

视频服务消费者: `CommentConsumer.handleCollectIncrement()` → `VideoServiceImpl.incrementCollectCount()` → SQL `GREATEST(collect_count + #{increment}, 0)`

---

## 4. 数据库表

### collect_folder（收藏夹表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| name | VARCHAR(50) | 收藏夹名称 |
| description | VARCHAR(200) | 描述 |
| is_public | TINYINT | 0-私有 1-公开 |
| video_count | INT | 冗余：视频数量 |
| cover_url | VARCHAR(500) | 封面图URL |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### video_collect（收藏记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| video_id | BIGINT | 视频ID |
| folder_id | BIGINT | 收藏夹ID |
| create_time | DATETIME | 收藏时间 |

唯一约束: `(user_id, video_id)` —— 同一个视频只能收藏一次
