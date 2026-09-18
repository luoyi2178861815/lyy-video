package com.lyy.video.service.impl;

import com.lyy.common.exception.BusinessException;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.FeedItemVO;
import com.lyy.video.feign.InteractionFeignClient;
import com.lyy.video.feign.UserFeignClient;
import com.lyy.video.feign.vo.UserBriefVO;
import com.lyy.video.mapper.VideoMapper;
import com.lyy.video.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注动态流服务实现
 *
 * 数据流（与设计文档第四节一一对应）：
 * (2) Feign 取关注ID列表 → (3) 并入「我自己」→ (4) 本地分页 →
 * (5) Feign 取本页作者资料 → (6) Feign 取本页点赞状态 → (7) 组装
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    /** 单页上限：防止前端传个巨大 pageSize 把表扫穿 */
    private static final int MAX_PAGE_SIZE = 20;

    private final VideoMapper videoMapper;
    private final InteractionFeignClient interactionFeignClient;
    private final UserFeignClient userFeignClient;

    @Override
    public PageResult getFeed(Long userId, int pageNum, int pageSize) {
        if (userId == null) {
            throw new BusinessException("用户不存在或者未登录");
        }

        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int page = Math.max(pageNum, 1);

        // (2) 关注ID列表。这一步故意不降级：拿不到关注列表＝内容不完整，
        //     降级成「只显示我自己」会静默丢内容，是隐蔽的错误；明确报错让前端提示重试更好。
        //     注意异常也要一并兜住：interaction-service 挂掉或网络不通时，Feign 是**抛异常**而不是返回 null，
        //     不兜的话会漏成 Spring 默认的 500 裸响应（没有 Result 信封），前端拿不到统一结构。
        Result<List<Long>> followResult;
        try {
            followResult = interactionFeignClient.getFollowingIds(userId);
        } catch (Exception e) {
            log.error("查询关注列表失败，中断本次动态流查询：{}", e.getMessage());
            throw new BusinessException("获取关注列表失败，请稍后重试");
        }
        if (followResult == null || followResult.getData() == null) {
            throw new BusinessException("获取关注列表失败，请稍后重试");
        }

        // (3) 作者集合 = 我关注的人 ∪ 我自己。用 Set 去重，覆盖「关注自己」和「自己已在关注列表中」两种情况
        Set<Long> authorIds = new HashSet<>(followResult.getData());
        authorIds.add(userId);
        List<Long> authors = new ArrayList<>(authorIds);

        // (4) 本地 SQL：唯一承担排序与分页的地方
        long total = videoMapper.countFeed(authors);
        if (total == 0) {
            return new PageResult(0, Collections.emptyList());
        }
        // offset 用 long 算：pageNum 是前端传的，(page-1)*size 在它极大时会 int 溢出成负数，
        // SQL 就变成 LIMIT -N 直接报错，而规格要求「超出总页数返回空 records 而不是报错」。
        // offset 一旦越过 total 就说明该页必为空，直接返回，连 SQL 都不用发
        long offsetLong = (long) (page - 1) * size;
        if (offsetLong >= total) {
            return new PageResult(total, Collections.emptyList());
        }
        List<Video> videos = videoMapper.selectFeedPage(authors, (int) offsetLong, size);
        if (videos.isEmpty()) {
            // 兜底：正常情况下走不到这（上面已按 total 挡掉），保留以防并发删除把本页清空
            return new PageResult(total, Collections.emptyList());
        }

        // (5)(6) 只传本页的 id（≤ size），不是全部
        Map<Long, UserBriefVO> authorMap = fetchAuthorMap(videos);
        Set<Long> likedIds = fetchLikedIds(userId, videos);

        // (7) 组装
        List<FeedItemVO> records = new ArrayList<>(videos.size());
        for (Video v : videos) {
            records.add(toItem(v, authorMap.get(v.getUserId()), likedIds));
        }
        return new PageResult(total, records);
    }

    /**
     * 批量取本页作者资料
     * 降级策略：失败时返回空 Map，卡片用「用户{id}」+ 空头像兜底——视频本身还在，主内容不受影响
     */
    private Map<Long, UserBriefVO> fetchAuthorMap(List<Video> videos) {
        List<Long> ids = videos.stream().map(Video::getUserId).distinct().collect(Collectors.toList());
        try {
            Result<List<UserBriefVO>> result = userFeignClient.getProfilesByIds(ids);
            if (result == null || result.getData() == null) {
                return Collections.emptyMap();
            }
            return result.getData().stream()
                    .collect(Collectors.toMap(UserBriefVO::getId, u -> u, (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量查询作者资料失败，动态流昵称回退为「用户{id}」：{}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 批量取本页点赞状态
     * @return 我赞过的 videoId 集合；返回 null 表示状态不可用（Feign 失败），
     *         调用方必须把该页所有卡片的 ♥ 置为禁用态
     */
    private Set<Long> fetchLikedIds(Long userId, List<Video> videos) {
        List<Long> videoIds = videos.stream().map(Video::getId).collect(Collectors.toList());
        try {
            Result<List<Long>> result = interactionFeignClient.getBatchLikeStatus(userId, videoIds);
            if (result == null || result.getData() == null) {
                return null;
            }
            return new HashSet<>(result.getData());
        } catch (Exception e) {
            log.warn("批量查询点赞状态失败，本页点赞按钮置为禁用态：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 组装单张卡片
     * @param likedIds 为 null 表示点赞状态不可用，此时 liked 固定 false 且 likeDisabled 置 true
     */
    private FeedItemVO toItem(Video v, UserBriefVO author, Set<Long> likedIds) {
        FeedItemVO vo = new FeedItemVO();
        vo.setVideoId(v.getId());
        vo.setTitle(v.getTitle());
        vo.setCoverUrl(v.getCoverUrl());
        vo.setDuration(v.getDuration());
        vo.setAuthorId(v.getUserId());
        vo.setAuthorName(author != null && author.getNickname() != null
                ? author.getNickname()
                : "用户" + v.getUserId());
        vo.setAuthorAvatar(author != null && author.getAvatar() != null ? author.getAvatar() : "");
        vo.setLikeCount(v.getLikeCount() == null ? 0L : v.getLikeCount());
        vo.setCommentCount(v.getCommentCount() == null ? 0L : v.getCommentCount());
        vo.setCollectCount(v.getCollectCount() == null ? 0L : v.getCollectCount());
        vo.setPublishTime(v.getCreateTime());

        boolean disabled = likedIds == null;
        vo.setLikeDisabled(disabled);
        vo.setLiked(!disabled && likedIds.contains(v.getId()));
        return vo;
    }
}
