# StarClip —— 微服务视频互动平台

视频播放网站，基于 Spring Cloud 微服务架构，集成 AI Agent 智能助手。

> **个人全栈项目** | 持续开发中

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.5 / Spring Cloud 2023.0.1 / Spring Cloud Alibaba 2023.0.1.0 |
| 网关 | Spring Cloud Gateway（WebFlux 路由 + 全局 JWT 过滤器） |
| 注册发现 & 配置中心 | Nacos |
| 数据库 | MySQL 8.0（5 库分库） + MyBatis |
| 缓存 | Redis（Lettuce 连接池） |
| 消息队列 | RabbitMQ（6 套 Topic Exchange） |
| 远程调用 | OpenFeign + LoadBalancer |
| 熔断降级 | Sentinel |
| AI | Spring AI Alibaba + 通义千问 (DashScope) + Elasticsearch 向量检索 |
| 视频处理 | JavaCV（元数据提取） |
| 语言 & 构建 | Java 17 + Maven 多模块 |

## 系统架构

```
客户端请求
    │
    ▼
┌────────────────────────────────────────┐
│  gateway-service :8000                 │
│  Spring Cloud Gateway                  │
│  ├─ 统一路由                            │
│  ├─ JWT 双 Token 认证（AT 15min / RT 7d）│
│  └─ 全局 CORS                          │
└──┬───────┬────────┬────────┬──────────┘
   │       │        │        │
   ▼       ▼        ▼        ▼          ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ user │ │video │ │inter │ │recom│ │ aigc │
│:15000│ │:8002 │ │action│ │mend │ │:1000 │
│      │ │      │ │:12000│ │:14000│ │      │
└──┬───┘ └──┬───┘ └──┬───┘ └──────┘ └──┬───┘
   │        │        │                  │
   └────────┼────────┘                  │
            ▼                           │
   ┌────────────────┐                   │
   │ Nacos :8848    │                   │
   │ 注册发现+配置中心│                   │
   └────────────────┘                   │
            ▼                           ▼
   ┌────────────────┐   ┌─────────────────────┐
   │ MySQL :3306    │   │ Elasticsearch :19200 │
   │ 5 个独立数据库  │   │ AI 向量检索          │
   └────────────────┘   └─────────────────────┘
   ┌────────────────┐
   │ Redis          │
   │ 缓存/Token/会话 │
   └────────────────┘
   ┌────────────────┐
   │ RabbitMQ :5672 │
   │ 异步消息解耦    │
   └────────────────┘
```

## 模块说明

| 模块 | 端口  | 职责 |
|------|-------|------|
| **common** | —     | 公共库：实体、DTO、统一返回、JWT 工具、异常处理、拦截器 |
| **gateway-service** | 8001  | API 网关：路由转发、JWT 鉴权（白名单 + Token Family 防重放） |
| **user-service** | 15000 | 用户服务：注册/登录、个人信息、双 Token 机制、经验值系统、**后台管理员账号与登录** |
| **video-service** | 8002  | 视频服务：上传/播放/搜索、JavaCV 元数据提取、观看进度看门狗、**视频审核治理** |
| **interaction-service** | 12000 | 互动服务：点赞/投币/收藏/评论/关注、Sentinel 熔断降级 |
| **aigc-service** | 1000  | AI 智能助手：RAG 检索增强 + Tool Calling + SSE 流式对话 + 持久化记忆 |
| **recommend-service** | 14000 | 推荐服务：（开发中）热门排行、个性化推荐 |

## 核心功能亮点

### 视频上传与安全
上传时基于 JavaCV 自动提取视频时长、分辨率、码率等元数据，上传即分析，无需异步任务。五层安全校验链（前端 accept 限制 → 扩展名白名单 → Content-Type 校验 → 魔数验证 → 大小限制 500MB）拦截恶意文件，单文件最大支持 500MB。

### 播放进度看门狗
前端每 5 秒上报播放进度。Redis 缓存最新进度 + JDK DelayQueue 实现延迟写库——每次上报刷新 Redis 缓存并将写库任务延期 10 秒，只有 10 秒内无新上报才认定用户停止观看并真正落库。DB 写入次数降低 90%+，保证最终一致性。

### 异步消息解耦
点赞、评论、收藏、关注、经验值等跨服务数据同步全部走 RabbitMQ。采用状态型 payload（LIKE/CANCEL 而非 ±1）保证幂等消费。CompletableFuture 并行编排 5 路 Feign 调用，综合 RPC 延迟从 1.2s 降至 300ms。

### 双 Token 认证 + 重放检测
Access Token（15min）+ Refresh Token（7d），基于 Token Family 轮转机制。同一族内 RT 只能使用一次，被重放时自动撤销整个族并强制用户重新登录。

### AI Agent 对话
基于 Spring AI Alibaba + 通义千问构建。支持 RAG 检索增强（Elasticsearch 向量检索 + Cross-Encoder 重排序）、Tool Calling 自动调用视频查询工具、SSE 流式推送、对话记忆 Redis 持久化（服务重启可恢复）、Nacos 热更新 Prompt 模板。

### 评论树
邻接表 + root_id 冗余字段，两条 SQL 构建两级评论树——先查一级评论，再 WHERE root_id IN (...) 批量查出所有子评论，Java 层按 root_id 分组组装，避免 N+1 递归查询。

### 缓存策略
- **视频详情**：Redis Hash 双缓存（统计信息 + 基础信息分离）
- **高热度视频点赞**：Redis BitMap 去重，异步落库
- **冷热分离**：热度公式 + 时间衰减，热数据走 Redis，冷数据降级查 DB
- **防穿透/击穿/雪崩**：布隆过滤器 + 空值缓存 + SETNX 锁 + TTL 随机偏移

### 多级缓存计数同步
L1 Redis 统计计数（高频读写）→ L2 Redis 基础数据 → L3 MySQL 持久化。互动事件通过 RabbitMQ 异步同步到各服务，保证最终一致性。

### 视频审核治理
上传的视频先进审核（`status=3`），管理员在独立后台通过或驳回，通过后才发布；已发布的视频可下架、下架后可重新上架。被驳回的视频，作者在「我的 → 审核区」能看到驳回原因（见 `yi-frontend`）。

视频状态机（`VideoStatusEnum`）：

| code | 枚举 | 含义 | C 端可见性 |
|------|------|------|-----------|
| 1 | `PUBLISHED` | 已发布 | 所有人 |
| 2 | `REMOVED` | 已下架 | 不可见 |
| 3 | `REVIEWING` | 审核中 | 仅作者本人 |
| 4 | `PRIVATE` | 私密 | 仅作者本人 |
| 5 | `REVIEW_REJECTED` | 审核不通过 | 仅作者本人 |

- **审核流水留痕**：`video_review` 表记录每一次治理动作（谁 / 何时 / 因何做了什么），驳回原因存在流水里而非冗余到视频表，任何一次判定都可追溯
- **状态机驱动的判定面**：后台详情页的操作栏完全由视频状态推导（审核中→通过·驳回、已发布→下架、已下架→重新上架、审核不通过→只读），非法操作在界面上根本不存在，后端状态守卫只作兜底而非主防线
- **管理端与 C 端完全隔离**：管理端用 `Admin-Token`（2h）、C 端用 `User-Token`（7d），两套密钥独立；网关 `AdminAuth` 过滤器校验后下发 `X-Admin-Id` / `X-Admin-Name`，下游用 `AdminContext` 承接，与 C 端的 `BaseContext` 是两个互不干扰的 ThreadLocal
- **网关路由顺序敏感**：`/api/admin/video/**` → video-service、`/api/admin/**` → user-service，两条路由的 `AdminAuth` **必须排在 `StripPrefix` 之前**，否则过滤器拿到的是被剥掉前缀的路径

## 快速开始

### 前置环境

| 组件 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 服务注册发现 + 配置中心（单机模式） |
| MySQL | 3306 | `root / 123456`，需创建 5 个数据库 |
| Redis | 6379 | 缓存、Token 家族、会话存储 |
| RabbitMQ | 5672 | `guest / guest`，异步消息 |
| Elasticsearch | 19200 | AI 向量检索（aigc-service 依赖） |

### 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS db_user;
CREATE DATABASE IF NOT EXISTS db_video;
CREATE DATABASE IF NOT EXISTS db_interaction;
CREATE DATABASE IF NOT EXISTS db_aigc;
CREATE DATABASE IF NOT EXISTS db_recommend;
```

各服务启动时 MyBatis 会自动建表（DDL 由 mapper XML 定义）。审核治理相关的两张表在 `resources/ddl/` 下，需先执行：

```bash
mysql -u root -p123456 < user-service/src/main/resources/ddl/admin.sql        # 后台管理员（db_user）
mysql -u root -p123456 < video-service/src/main/resources/ddl/video_review.sql # 视频治理流水（db_video）
```

`admin.sql` 里刻意不写种子数据——BCrypt 密文必须在运行期由 `BCryptPasswordEncoder` 生成，手写密文一旦有误就是「密码正确却登不进去」的难查故障。默认超管由 user-service 首次启动时的 `AdminSeeder` 播种（表为空才写，幂等）。

### Nacos 配置

在 Nacos 控制台（http://localhost:8848/nacos）创建以下配置：

- `common-redis.yml` —— Redis 连接配置（所有服务共享）
- `sentinel-common.yml` —— Sentinel 熔断规则（interaction-service）
- `aigc-service.yaml` —— AI 会话配置（会话标题、默认提示词）
- `system-chat-message.txt` —— System Prompt（aigc-service，支持热更新）

### 构建 & 启动

```bash
# 1. 克隆项目
git clone https://github.com/luoyi2178861815/StarClip.git
cd StarClip

# 2. 构建全部模块
mvn clean install -DskipTests

# 3. 按顺序启动（先启动基础设施，再启动业务服务）
mvn spring-boot:run -pl gateway-service     # 网关 :8000
mvn spring-boot:run -pl user-service        # 用户 :15000
mvn spring-boot:run -pl video-service       # 视频 :8002
mvn spring-boot:run -pl interaction-service # 互动 :12000
mvn spring-boot:run -pl aigc-service        # AI   :1000
mvn spring-boot:run -pl recommend-service   # 推荐 :14000
```

### 架构图 & API 文档

启动后访问：`http://localhost:{port}/doc.html`（Knife4j / Swagger UI）

### 管理后台前端

管理后台是独立前端工程 `admin-frontend`（Vue 3 + naive-ui + UnoCSS + Vite），与 `lyy-video/` 同级存放、不在本仓库内：

```bash
cd admin-frontend
npm install
npm run dev          # http://localhost:3001
```

默认账号 `admin / 123456`（由 user-service 首次启动时播种）。Vite 把 `/api` 代理到网关 `:8001`，所以**后端进程必须先起**，否则页面能打开但接口全红。

## 项目结构

```
lyy-video/
├── common/                   # 公共模块（实体、DTO、工具类、拦截器）
├── gateway-service/          # API 网关（路由 + JWT 过滤器 + AdminAuth）
├── user-service/             # 用户服务（注册/登录/Token Family/管理员账号）
├── video-service/            # 视频服务（上传/播放/JavaCV/看门狗/审核治理）
├── interaction-service/      # 互动服务（点赞/评论/收藏/关注/Sentinel）
├── aigc-service/             # AI 服务（RAG/Tool Calling/SSE/会话记忆）
├── recommend-service/        # 推荐服务（开发中）
└── videos/                   # 上传文件存储目录
```

本仓库只含后端微服务。本地工作区中与 `lyy-video/` 同级还有两个前端工程（不在本仓库内）：

- `yi-frontend/` —— C 端，开发端口 `:3000`，含「我的 → 审核区」
- `admin-frontend/` —— 管理后台，开发端口 `:3001`

对应的一键启动脚本 `start-frontend.bat` / `start-admin-frontend.bat` 同样放在工作区根目录。