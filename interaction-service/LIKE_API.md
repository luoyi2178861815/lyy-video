# Interaction Service - 视频点赞模块 API 文档

> 基础路径：`http://localhost:<port>/interact/like`

---

## 一、接口列表

| 方法 | 路径 | 说明 | 登录 |
|------|------|------|------|
| `POST` | `/interact/like/{videoId}/like` | 点赞/取消点赞(toggle) | 需要 |
| `GET` | `/interact/like/list` | 查询用户点赞列表（分页） | 需要 |

---

## 二、接口详情

### 2.1 点赞/取消点赞
```
POST /interact/like/{videoId}/like
Header: User-Token: <token>
```
**响应（点赞）：**
```json
{"code": 1, "data": {"liked": true}}
```
**响应（取消）：**
```json
{"code": 1, "data": {"liked": false}}
```
**错误：**
```json
{"code": 0, "msg": "视频不存在或已删除"}
```

---

### 2.2 查询点赞列表（分页）
```
GET /interact/like/list?pageNum=1&pageSize=10
Header: User-Token: <token>
```

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页数量 |

**响应：**
```json
{
    "code": 1,
    "data": {
        "total": 25,
        "records": [
            {
                "videoId": 1001,
                "title": "Spring Cloud 微服务实战",
                "coverUrl": "https://xxx/cover.jpg",
                "duration": 1847,
                "playCount": 12000,
                "likeCount": 356,
                "commentCount": 89,
                "authorId": 5,
                "authorName": "UP主小明",
                "authorAvatar": "https://xxx/avatar.jpg",
                "likeTime": "2025-06-08 15:30:00"
            }
        ]
    }
}
```
> video-service 不可用时视频信息降级为空，不中断主流程。

---

## 三、数据库 DDL

```sql
-- 视频点赞记录表（如未建）
create table video_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    video_id    bigint                             not null comment '视频ID',
    user_id     bigint                             not null comment '点赞用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '点赞时间',
    constraint uk_video_user
        unique (video_id, user_id)
)
    comment '视频点赞记录表';
```

---

## 四、改动清单

| 层级 | 文件 | 变更 |
|------|------|------|
| VO | `interaction/entity/vo/VideoLikeVO.java` | 填字段：视频信息 + UP主信息 + 点赞时间 |
| Mapper | `interaction/mapper/VideoLikeMapper.java` | +selectUserLikePage, +selectUserLikeCount |
| XML | `interaction/.../mapper/VideoLikeMapper.xml` | +分页查询 SQL, +count SQL |
| Feign | `interaction/feign/VideoFeignClient.java` | +getVideosByIds 批量查视频 |
| Service | `interaction/service/VideoLikeService.java` | +getUserLikePage |
| ServiceImpl | `interaction/.../VideoLikeServiceImpl.java` | 实现分页查询 + 视频/用户信息富化 + Feign 降级 |
| Controller | `interaction/controller/VideoLikeController.java` | +GET /list 接口 |
| Mapper | `video/mapper/VideoMapper.java` | +selectByIds, +exists |
| XML | `video/.../VideoMapper.xml` | +selectByIds SQL, +exists SQL |
| Service | `video/service/VideoService.java` | +getVideosByIds |
| ServiceImpl | `video/.../VideoServiceImpl.java` | 实现批量查询，exists 改用 count 优化 |
| Controller | `video/controller/PlayVideoController.java` | +GET /video/batch 批量查询接口 |

---

## 五、单元测试方法

### 5.1 VideoLikeServiceImplTest

```java
@ExtendWith(MockitoExtension.class)
class VideoLikeServiceImplTest {

    @Mock private VideoLikeMapper videoLikeMapper;
    @Mock private VideoFeignClient videoFeignClient;
    @Mock private UserFeignClient userFeignClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private VideoLikeServiceImpl videoLikeService;

    // ---- likeVideo ----

    @Test
    void likeVideo_未点赞_执行点赞返回true() {
        when(videoFeignClient.exists(1L)).thenReturn(Result.success(1));
        when(videoLikeMapper.exits(1L, 1L)).thenReturn(0);

        boolean liked = videoLikeService.likeVideo(1L, 1L);

        assertTrue(liked);
        verify(videoLikeMapper).like(any(VideoLike.class));
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void likeVideo_已点赞_执行取消返回false() {
        when(videoFeignClient.exists(1L)).thenReturn(Result.success(1));
        when(videoLikeMapper.exits(1L, 1L)).thenReturn(1);

        boolean liked = videoLikeService.likeVideo(1L, 1L);

        assertFalse(liked);
        verify(videoLikeMapper).deletelike(1L, 1L);
    }

    @Test
    void likeVideo_视频不存在_抛异常() {
        when(videoFeignClient.exists(999L)).thenReturn(Result.success(0));

        assertThrows(VideoNotFoundException.class,
                () -> videoLikeService.likeVideo(999L, 1L));
    }

    @Test
    void likeVideo_videoId为null_抛异常() {
        assertThrows(BusinessException.class,
                () -> videoLikeService.likeVideo(null, 1L));
    }

    // ---- getUserLikePage ----

    @Test
    void getUserLikePage_有数据_返回含视频和UP主信息的列表() {
        // 点赞记录
        VideoLike like = new VideoLike(1L, 1001L, 1L, LocalDateTime.now());
        when(videoLikeMapper.selectUserLikePage(eq(1L), anyInt(), anyInt()))
                .thenReturn(List.of(like));
        when(videoLikeMapper.selectUserLikeCount(1L)).thenReturn(1);

        // 视频信息（Feign 返回 Map，模拟 JSON 反序列化）
        Map<String, Object> videoMap = new HashMap<>();
        videoMap.put("id", 1001);
        videoMap.put("title", "测试视频");
        videoMap.put("coverUrl", "https://xxx/cover.jpg");
        videoMap.put("duration", 120);
        videoMap.put("playCount", 1000);
        videoMap.put("likeCount", 50);
        videoMap.put("commentCount", 10);
        videoMap.put("userId", 5);
        Result<List<Map<String, Object>>> videoResult = Result.success(List.of(videoMap));
        when(videoFeignClient.getVideosByIds(anyList())).thenReturn(videoResult);

        // UP主信息
        CommentUserVO author = new CommentUserVO(5L, "u5", "UP主小明", "https://xxx/avatar.jpg");
        Result<List<CommentUserVO>> userResult = Result.success(List.of(author));
        when(userFeignClient.getProfilesByIds(anyList())).thenReturn(userResult);

        PageResult page = videoLikeService.getUserLikePage(1L, 1, 10);

        assertEquals(1, page.getTotal());
        List<VideoLikeVO> records = page.getRecords();
        assertEquals(1, records.size());
        VideoLikeVO vo = records.get(0);
        assertEquals(Long.valueOf(1001), vo.getVideoId());
        assertEquals("测试视频", vo.getTitle());
        assertEquals("UP主小明", vo.getAuthorName());
    }

    @Test
    void getUserLikePage_无数据_返回空列表() {
        when(videoLikeMapper.selectUserLikePage(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(videoLikeMapper.selectUserLikeCount(anyLong())).thenReturn(0);

        PageResult page = videoLikeService.getUserLikePage(1L, 1, 10);

        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void getUserLikePage_Feign异常_降级不中断() {
        VideoLike like = new VideoLike(1L, 1001L, 1L, LocalDateTime.now());
        when(videoLikeMapper.selectUserLikePage(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(like));
        when(videoLikeMapper.selectUserLikeCount(anyLong())).thenReturn(1);
        when(videoFeignClient.getVideosByIds(anyList()))
                .thenThrow(new RuntimeException("Feign 超时"));

        PageResult page = videoLikeService.getUserLikePage(1L, 1, 10);

        assertEquals(1, page.getTotal());
        VideoLikeVO vo = (VideoLikeVO) page.getRecords().get(0);
        assertNull(vo.getTitle()); // 降级后视频信息为空
        assertNotNull(vo.getLikeTime()); // 点赞时间还在
    }
}
```

### 5.2 VideoLikeControllerTest

```java
@WebMvcTest(VideoLikeController.class)
class VideoLikeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private VideoLikeService videoLikeService;

    @Test
    void likeVideo_成功() throws Exception {
        when(videoLikeService.likeVideo(eq(1L), anyLong())).thenReturn(true);

        mockMvc.perform(post("/interact/like/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.liked").value(true));
    }

    @Test
    void getUserLikeList_正常分页_返回数据() throws Exception {
        VideoLikeVO vo = VideoLikeVO.builder()
                .videoId(1001L).title("测试").likeTime(LocalDateTime.now()).build();
        PageResult page = new PageResult(1, List.of(vo));
        when(videoLikeService.getUserLikePage(anyLong(), eq(1), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/interact/like/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].videoId").value(1001));
    }
}
```

### 5.3 运行说明

```bash
# 单独运行点赞模块测试
mvn test -pl interaction-service -Dtest=VideoLikeServiceImplTest

# 运行指定测试方法
mvn test -pl interaction-service -Dtest=VideoLikeServiceImplTest#getUserLikePage_有数据_返回含视频和UP主信息的列表
```
