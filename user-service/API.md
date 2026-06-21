# User Service API 接口文档

> 基础路径 C端：`http://localhost:15000/user`  
> 基础路径 B端：`http://localhost:15000/admin/user`  

---

## 一、C端接口（前台用户）

### 1.1 用户注册
```
POST /user/register
```
**请求体：**
```json
{
    "username": "zhangsan",
    "password": "123456",
    "nickname": "张三",
    "phone": "13800138000"
}
```
**响应：**
```json
{"code": 1, "msg": null, "data": "注册成功"}
```
| 字段 | 说明 |
|------|------|
| username | 必填，登录账号（唯一） |
| password | 必填 |

---

### 1.2 用户登录
```
POST /user/login
```
**请求体：**
```json
{
    "username": "zhangsan",
    "password": "123456"
}
```
**响应：**
```json
{
    "code": 1,
    "data": {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "token": "eyJhbGciOi..."
    }
}
```

---

### 1.3 获取个人主页 / UP主信息
```
GET /user/profile/{userId}
```
**响应：**
```json
{
    "code": 1,
    "data": {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "",
        "sign": "",
        "phone": "138****8000",
        "status": 1,
        "level": 1,
        "exp": 0,
        "followCount": 0,
        "fansCount": 0,
        "likeTotal": 0,
        "playTotal": 0,
        "createTime": "2025-01-01 12:00:00",
        "updateTime": "2025-01-01 12:00:00"
    }
}
```

---

### 1.4 修改个人资料 （需登录）
```
PUT /user/profile
Header: User-Token: <token>
```
**请求体（全部可选）：**
```json
{
    "nickname": "新昵称",
    "avatar": "https://xxx.com/avatar.jpg",
    "sign": "个性签名",
    "phone": "13900139000"
}
```
**校验规则：**

| 字段 | 限制 |
|------|------|
| nickname | 最长 50 字符 |
| avatar | 最长 255 字符 |
| sign | 最长 255 字符 |
| phone | 空字符串或 11 位手机号 |

**响应：**
```json
{"code": 1, "data": "修改成功"}
```

---

### 1.5 修改密码 （需登录）
```
PUT /user/password
Header: User-Token: <token>
```
**请求体：**
```json
{
    "oldPassword": "123456",
    "newPassword": "654321"
}
```
**校验规则：**

| 字段 | 限制 |
|------|------|
| oldPassword | 不能为空 |
| newPassword | 不能为空，长度 6-50 位 |

**响应：**
```json
{"code": 1, "data": "密码修改成功"}
```
**错误：**
```json
{"code": 0, "msg": "旧密码不正确"}
```

---

### 1.6 注销账号 （需登录）
```
DELETE /user/account
Header: User-Token: <token>
```
**响应：**
```json
{"code": 1, "data": "账号已注销"}
```

---

## 二、B端接口（管理后台）

### 2.1 分页查询用户列表
```
GET /admin/user/page?pageNum=1&pageSize=10&keyword=zhang
```

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页数量 |
| keyword | 否 | — | 按 username / nickname 模糊搜索 |

**响应：**
```json
{
    "code": 1,
    "data": {
        "total": 100,
        "records": [
            {
                "id": 1,
                "username": "zhangsan",
                "nickname": "张三",
                "status": 1,
                "level": 1,
                "exp": 0,
                "followCount": 10,
                "fansCount": 5,
                "likeTotal": 200,
                "playTotal": 5000,
                "createTime": "2025-01-01 12:00:00",
                "updateTime": "2025-01-01 12:00:00"
            }
        ]
    }
}
```

---

### 2.2 查看用户详情
```
GET /admin/user/{id}
```
**响应：** 同 C端 1.3 的 data 结构

---

### 2.3 新增用户
```
POST /admin/user
```
**请求体：**
```json
{
    "username": "lisi",
    "password": "123456",
    "nickname": "李四",
    "avatar": "",
    "sign": "",
    "phone": ""
}
```
**校验规则：**

| 字段 | 限制 |
|------|------|
| username | 必填，3-50 字符 |
| password | 必填，6-50 字符 |
| nickname | 最长 50 字符 |
| avatar | 最长 255 字符 |
| sign | 最长 255 字符 |

**响应：**
```json
{"code": 1, "data": "新增成功"}
```
**错误：**
```json
{"code": 0, "msg": "该账号已被注册"}
```

---

### 2.4 编辑用户
```
PUT /admin/user
```
**请求体（除 id 外全部可选）：**
```json
{
    "id": 1,
    "password": "newpwd123",
    "nickname": "新昵称",
    "avatar": "",
    "sign": "",
    "phone": "",
    "status": 0,
    "level": 5,
    "exp": 1000
}
```
**校验规则：**

| 字段 | 限制 |
|------|------|
| id | 必填 |
| password | 长度 6-50 位（不传则不修改） |
| nickname | 最长 50 字符 |
| avatar | 最长 255 字符 |
| sign | 最长 255 字符 |

**响应：**
```json
{"code": 1, "data": "编辑成功"}
```

---

### 2.5 封禁 / 解封用户
```
PUT /admin/user/{id}/status?status=0
```

| 参数 | 说明 |
|------|------|
| status=1 | 解封（正常） |
| status=0 | 封禁 |

**响应：**
```json
{"code": 1, "data": "已封禁"}
```

---

### 2.6 删除用户
```
DELETE /admin/user/{id}
```
**响应：**
```json
{"code": 1, "data": "删除成功"}
```

---

## 三、通用错误码

| code | 含义 |
|------|------|
| 1 | 成功 |
| 0 | 业务失败（msg 中有具体描述） |
| 401 | 账号或密码错误 |
| 403 | 账号已被封禁 |

**参数校验失败示例：**
```json
{
    "code": 0,
    "msg": "username: 账号长度3-50位; password: 密码不能为空"
}
```

---

