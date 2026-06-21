# Interaction Service - 评论模块 API 文档

> 基础路径：`http://localhost:<port>/interact/comment`

---

## 一、接口列表

| 方法 | 路径 | 说明 | 登录 |
|------|------|------|------|
| `POST` | `/interact/comment/add` | 添加评论 | 需要 |
| `GET` | `/interact/comment/tree` | 获取评论树（分页） | 可选 |
| `DELETE` | `/interact/comment/{id}` | 删除评论 | 需要 |
| `PUT` | `/interact/comment/{id}` | 编辑评论 | 需要 |
| `POST` | `/interact/comment/{id}/like` | 点赞/取消点赞(toggle) | 需要 |

---

## 二、接口详情

### 2.1 添加评论
```
POST /interact/comment/add
Header: User-Token: <token>
```
**请求体：**
```json
{
    "videoId": 1001,
    "parentId": 0,
    "content": "UP主讲得真好！"
}
```
| 字段 | 必填 | 说明 |
|------|------|------|
| videoId | 是 | 视频ID |
| parentId | 是 | 0=根评论，非0=回复某条评论 |
| replyToUserId | 否 | 被回复的用户ID（回复时可选） |
| content | 是 | 评论内容，最长1000字 |

**响应：**
```json
{"code": 1, "data": "添加评论成功！"}
```

---

### 2.2 获取评论树（分页）
```
GET /interact/comment/tree?videoId=1001&pageNum=1&pageSize=10
Header: User-Token: <token> (可选)
```

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| videoId | 是 | — | 视频ID |
| pageNum | 否 | 1 | 页码（根评论分页） |
| pageSize | 否 | 10 | 每页根评论数 |

**响应：**
```json
{
    "code": 1,
    "data": {
        "videoId": 1001,
        "total": 25,
        "commentTrees": [
            {
                "id": 1,
                "videoId": 1001,
                "userId": 5,
                "username": "zhangsan",
                "nickname": "张三",
                "avatar": "https://xxx/avatar.jpg",
                "parentId": 0,
                "rootId": 1,
                "replyToUserId": null,
                "replyToUsername": null,
                "replyToNickname": null,
                "content": "UP主讲得真好！",
                "likeCount": 3,
                "liked": true,
                "status": 1,
                "createTime": "2025-06-01 10:00:00",
                "children": [
                    {
                        "id": 3,
                        "parentId": 1,
                        "rootId": 1,
                        "userId": 7,
                        "username": "lisi",
                        "nickname": "李四",
                        "avatar": "",
                        "replyToUserId": 5,
                        "replyToUsername": "zhangsan",
                        "replyToNickname": "张三",
                        "content": "同感！",
                        "likeCount": 0,
                        "liked": false,
                        "children": [],
                        ...
                    }
                ]
            }
        ]
    }
}
```
> 未登录时 `liked` 始终为 `false`，`username/nickname/avatar` 在 user-service 不可用时降级为空字符串。

---

### 2.3 删除评论（仅作者）
```
DELETE /interact/comment/{id}
Header: User-Token: <token>
```
**响应：**
```json
{"code": 1, "data": "删除成功"}
```
**错误：**
```json
{"code": 0, "msg": "只能删除自己的评论"}
```

---

### 2.4 编辑评论（仅作者）
```
PUT /interact/comment/{id}
Header: User-Token: <token>
```
**请求体：**
```json
{"content": "修改后的内容"}
```
**响应：**
```json
{"code": 1, "data": "编辑成功"}
```

---

### 2.5 点赞/取消点赞（toggle）
```
POST /interact/comment/{id}/like
Header: User-Token: <token>
```
**响应（已点赞→取消）：**
```json
{"code": 1, "data": {"liked": false}}
```
**响应（未点赞→点赞）：**
```json
{"code": 1, "data": {"liked": true}}
```

---

## 三、数据库 DDL

```sql
-- 评论点赞记录表（如未建）
create table comment_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    comment_id  bigint                             not null comment '评论ID',
    user_id     bigint                             not null comment '点赞用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '点赞时间',
    constraint uk_comment_user
        unique (comment_id, user_id)
)
    comment '评论点赞记录表';
```

---

## 四、改动清单

| 层级 | 文件 | 变更 |
|------|------|------|
| PO | `entity/po/CommentLike.java` | 新增，点赞记录实体 |
| VO | `entity/vo/CommentVO.java` | 新增，评论展示VO（含用户信息+子回复） |
| VO | `entity/vo/CommentUserVO.java` | 新增，Feign 用户最小信息 |
| VO | `entity/vo/CommentTreesVO.java` | 改造，新增 total 字段，List<Comment> → List<CommentVO> |
| Feign | `feign/UserFeignClient.java` | 新增，调 user-service 批量查用户 |
| Mapper | `mapper/CommentMapper.java` | +9 方法 |
| Mapper | `mapper/CommentMapper.xml` | +8 SQL |
| Service | `service/CommentService.java` | 改造接口，返回类型+新增删除/编辑/点赞 |
| Service | `service/impl/CommentServiceImpl.java` | 实现全部逻辑，含用户信息富化、Feign 降级 |
| Controller | `controller/CommentController.java` | +4 接口，getCommentTree 改分页 |
| user-service | `mapper/UserMapper.java/xml` | +selectByIds 批量查询 |
| user-service | `service/UserService.java/impl` | +getProfilesByIds |
| user-service | `controller/UserController.java` | +GET /user/profiles/batch |

---

## 五、单元测试方法

以下为 Spring Boot Test + Mockito 的测试方法骨架。

### 5.1 CommentServiceImplTest

```java
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentMapper commentMapper;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private UserFeignClient userFeignClient;
    @InjectMocks private CommentServiceImpl commentService;

    // ---- addComment ----

    @Test
    void addComment_根评论_插入并回写RootId() {
        CommentAddDTO dto = new CommentAddDTO(1001L, 0L, null, "测试评论");
        BaseContext.setCurrentId(1L);

        commentService.addComment(dto);

        verify(commentMapper).insert(any(Comment.class));
        verify(commentMapper).updateRootId(anyLong(), anyLong());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void addComment_子回复_查父评论获取RootId() {
        CommentAddDTO dto = new CommentAddDTO(1001L, 5L, null, "回复");
        BaseContext.setCurrentId(1L);
        Comment parent = new Comment();
        parent.setId(5L);
        parent.setRootId(3L);
        when(commentMapper.selectByCommentId(5L)).thenReturn(parent);

        commentService.addComment(dto);

        verify(commentMapper).insert(argThat(c -> c.getRootId().equals(3L)));
    }

    @Test
    void addComment_parentId为null_抛BusinessException() {
        CommentAddDTO dto = new CommentAddDTO(1001L, null, null, "内容");
        BaseContext.setCurrentId(1L);

        assertThrows(BusinessException.class, () -> commentService.addComment(dto));
    }

    // ---- getCommentTree ----

    @Test
    void getCommentTree_有数据_返回树结构含用户信息() {
        Long videoId = 1001L;
        Comment root = mockComment(1L, 0L, 1L);
        Comment reply = mockComment(2L, 1L, 1L);
        reply.setReplyToUserId(5L);

        when(commentMapper.selectRootPage(eq(videoId), anyInt(), anyInt()))
                .thenReturn(List.of(root));
        when(commentMapper.selectRootCount(videoId)).thenReturn(1);
        when(commentMapper.selectByRootIds(List.of(1L))).thenReturn(List.of(reply));

        CommentUserVO author = new CommentUserVO(1L, "u1", "昵称1", "");
        CommentUserVO replyTo = new CommentUserVO(5L, "u5", "昵称5", "");
        Result<List<CommentUserVO>> feignResult = Result.success(List.of(author, replyTo));
        when(userFeignClient.getProfilesByIds(anyList())).thenReturn(feignResult);

        CommentTreesVO result = commentService.getCommentTree(videoId, 1, 10, 1L);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCommentTrees().size());
        assertEquals("昵称1", result.getCommentTrees().get(0).getNickname());
        assertEquals(1, result.getCommentTrees().get(0).getChildren().size());
    }

    @Test
    void getCommentTree_无数据_返回空树() {
        when(commentMapper.selectRootPage(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(commentMapper.selectRootCount(anyLong())).thenReturn(0);

        CommentTreesVO result = commentService.getCommentTree(1001L, 1, 10, null);

        assertEquals(0, result.getTotal());
        assertTrue(result.getCommentTrees().isEmpty());
    }

    @Test
    void getCommentTree_Feign异常_降级不中断() {
        when(commentMapper.selectRootPage(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(mockComment(1L, 0L, 1L)));
        when(commentMapper.selectRootCount(anyLong())).thenReturn(1);
        when(commentMapper.selectByRootIds(anyList())).thenReturn(List.of());
        when(userFeignClient.getProfilesByIds(anyList()))
                .thenThrow(new RuntimeException("Feign 超时"));

        CommentTreesVO result = commentService.getCommentTree(1001L, 1, 10, null);

        assertNotNull(result);
        assertEquals(1, result.getCommentTrees().size());
        assertNull(result.getCommentTrees().get(0).getNickname()); // 降级后为空
    }

    // ---- deleteComment ----

    @Test
    void deleteComment_作者删除自己的评论_成功() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(1L);
        comment.setStatus(1);
        when(commentMapper.selectByCommentId(1L)).thenReturn(comment);

        commentService.deleteComment(1L, 1L);

        verify(commentMapper).deleteById(1L);
    }

    @Test
    void deleteComment_非作者删除_抛异常() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(2L); // 作者是2
        comment.setStatus(1);
        when(commentMapper.selectByCommentId(1L)).thenReturn(comment);

        assertThrows(BusinessException.class,
                () -> commentService.deleteComment(1L, 1L)); // 1想删2的评论
        verify(commentMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteComment_评论不存在_抛异常() {
        when(commentMapper.selectByCommentId(1L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> commentService.deleteComment(1L, 1L));
    }

    // ---- editComment ----

    @Test
    void editComment_作者编辑_成功() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(1L);
        comment.setStatus(1);
        when(commentMapper.selectByCommentId(1L)).thenReturn(comment);

        commentService.editComment(1L, 1L, "新内容");

        verify(commentMapper).updateContent(1L, "新内容");
    }

    // ---- likeComment ----

    @Test
    void likeComment_未点赞_执行点赞返回true() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setStatus(1);
        when(commentMapper.selectByCommentId(1L)).thenReturn(comment);
        when(commentMapper.selectLikeExists(1L, 1L)).thenReturn(0);

        boolean liked = commentService.likeComment(1L, 1L);

        assertTrue(liked);
        verify(commentMapper).insertLike(any(CommentLike.class));
        verify(commentMapper).incrLikeCount(1L);
    }

    @Test
    void likeComment_已点赞_执行取消返回false() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setStatus(1);
        when(commentMapper.selectByCommentId(1L)).thenReturn(comment);
        when(commentMapper.selectLikeExists(1L, 1L)).thenReturn(1);

        boolean liked = commentService.likeComment(1L, 1L);

        assertFalse(liked);
        verify(commentMapper).deleteLike(1L, 1L);
        verify(commentMapper).decrLikeCount(1L);
    }

    @Test
    void likeComment_评论已删除_抛异常() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setStatus(0); // 已删除
        when(commentMapper.selectByCommentId(1L)).thenReturn(comment);

        assertThrows(BusinessException.class,
                () -> commentService.likeComment(1L, 1L));
    }

    // ---- 辅助 ----

    private Comment mockComment(Long id, Long parentId, Long rootId) {
        Comment c = new Comment();
        c.setId(id);
        c.setVideoId(1001L);
        c.setUserId(1L);
        c.setParentId(parentId);
        c.setRootId(rootId);
        c.setContent("内容" + id);
        c.setLikeCount(0);
        c.setStatus(1);
        c.setCreateTime(LocalDateTime.now());
        return c;
    }
}
```

### 5.2 CommentControllerTest

```java
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CommentService commentService;

    @Test
    void addComment_校验失败_返回错误() throws Exception {
        String body = "{\"videoId\":null,\"parentId\":null,\"content\":\"\"}";

        mockMvc.perform(post("/interact/comment/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("视频ID不能为空")));
    }

    @Test
    void getCommentTree_正常分页_返回数据() throws Exception {
        CommentTreesVO vo = new CommentTreesVO();
        vo.setVideoId(1001L);
        vo.setTotal(0);
        vo.setCommentTrees(List.of());
        when(commentService.getCommentTree(eq(1001L), eq(1), eq(10), any()))
                .thenReturn(vo);

        mockMvc.perform(get("/interact/comment/tree")
                        .param("videoId", "1001")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.videoId").value(1001));
    }

    @Test
    void deleteComment_成功() throws Exception {
        mockMvc.perform(delete("/interact/comment/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void likeComment_toggle_返回liked() throws Exception {
        when(commentService.likeComment(1L, anyLong())).thenReturn(true);

        mockMvc.perform(post("/interact/comment/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));
    }
}
```

### 5.3 运行说明

```bash
# 单独运行评论模块测试
mvn test -pl interaction-service

# 运行指定测试类
mvn test -pl interaction-service -Dtest=CommentServiceImplTest

# 运行指定测试方法
mvn test -pl interaction-service -Dtest=CommentServiceImplTest#addComment_根评论_插入并回写RootId
```

> 测试依赖：需要 `spring-boot-starter-test`（含 JUnit 5 + Mockito），interaction-service 的 pom.xml 中需确认已引入。
