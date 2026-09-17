package com.lyy.video.service.admin.impl;

import com.lyy.common.constant.RedisKey;
import com.lyy.common.context.AdminContext;
import com.lyy.common.enums.VideoReviewActionEnum;
import com.lyy.common.enums.VideoStatusEnum;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.result.PageResult;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.po.VideoReview;
import com.lyy.video.entity.vo.AdminVideoDetailVO;
import com.lyy.video.entity.vo.AdminVideoVO;
import com.lyy.video.entity.vo.ReviewRecordVO;
import com.lyy.video.feign.UserFeignClient;
import com.lyy.video.feign.vo.UserBriefVO;
import com.lyy.video.mapper.VideoMapper;
import com.lyy.video.mapper.VideoReviewMapper;
import com.lyy.video.service.admin.AdminVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端视频服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminVideoServiceImpl implements AdminVideoService {

    /**
     * 动作 → [允许的当前状态, 目标状态]
     * 四个治理动作共用的状态守卫，避免每个动作各写一遍 if
     */
    private static final Map<VideoReviewActionEnum, int[]> ACTION_RULES = Map.of(
            VideoReviewActionEnum.APPROVE, new int[]{3, 1},
            VideoReviewActionEnum.REJECT,  new int[]{3, 5},
            VideoReviewActionEnum.OFFLINE, new int[]{1, 2},
            VideoReviewActionEnum.ONLINE,  new int[]{2, 1}
    );

    /** 状态守卫失败时的提示语，按动作区分，便于前端直接展示 */
    private static final Map<VideoReviewActionEnum, String> INVALID_STATE_MSG = Map.of(
            VideoReviewActionEnum.APPROVE, "该视频不在待审核状态",
            VideoReviewActionEnum.REJECT,  "该视频不在待审核状态",
            VideoReviewActionEnum.OFFLINE, "该视频当前不可下架",
            VideoReviewActionEnum.ONLINE,  "该视频当前不可上架"
    );

    private final VideoMapper videoMapper;
    private final VideoReviewMapper videoReviewMapper;
    private final UserFeignClient userFeignClient;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    public PageResult pageVideos(Integer status, int pageNum, int pageSize) {
        // 默认只看待审核，管理员的日常入口就是这里
        int filterStatus = status != null ? status : VideoStatusEnum.REVIEWING.getCode();
        int offset = (pageNum - 1) * pageSize;

        List<Video> videos = videoMapper.pageVideosForAdmin(filterStatus, offset, pageSize);
        long total = videoMapper.countVideosForAdmin(filterStatus);

        Map<Long, String> nicknameMap = loadNicknames(
                videos.stream().map(Video::getUserId).collect(Collectors.toList()));

        List<AdminVideoVO> records = videos.stream().map(v -> {
            AdminVideoVO vo = new AdminVideoVO();
            BeanUtils.copyProperties(v, vo);
            vo.setStatusDesc(VideoStatusEnum.fromCode(v.getStatus()).getDesc());
            vo.setAuthorName(nicknameMap.get(v.getUserId()) != null
                    ? nicknameMap.get(v.getUserId()) : "未知用户");
            return vo;
        }).collect(Collectors.toList());

        return new PageResult(total, records);
    }

    @Override
    public AdminVideoDetailVO getDetail(Long videoId) {
        // 复用已有的 getVideoInfo（SQL 本就无状态过滤，正合管理端需求）
        Video video = videoMapper.getVideoInfo(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        AdminVideoDetailVO vo = new AdminVideoDetailVO();
        BeanUtils.copyProperties(video, vo);
        vo.setStatusDesc(VideoStatusEnum.fromCode(video.getStatus()).getDesc());

        Map<Long, String> nicknameMap = loadNicknames(List.of(video.getUserId()));
        vo.setAuthorName(nicknameMap.get(video.getUserId()) != null
                ? nicknameMap.get(video.getUserId()) : "未知用户");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void govern(Long videoId, VideoReviewActionEnum action, String reason) {
        Video video = videoMapper.getVideoInfo(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        int current = video.getStatus() == null ? 0 : video.getStatus();

        // ① 状态守卫：当前状态是否允许该动作
        int[] rule = ACTION_RULES.get(action);
        if (rule == null) {
            throw new BusinessException("不支持的治理动作");
        }
        if (current != rule[0]) {
            throw new BusinessException(INVALID_STATE_MSG.get(action));
        }
        // 驳回必须给出原因，否则作者看到的是一个没有信息的驳回
        if (action == VideoReviewActionEnum.REJECT
                && (reason == null || reason.trim().isEmpty())) {
            throw new BusinessException("驳回原因不能为空");
        }

        // ② 更新状态 + 写流水
        int target = rule[1];
        videoMapper.updateStatus(videoId, target);

        VideoReview record = new VideoReview();
        record.setVideoId(videoId);
        record.setAdminId(AdminContext.getAdminId());
        record.setAdminName(AdminContext.getAdminName());
        record.setAction(action.getCode());
        record.setReason(reason);
        record.setCreateTime(LocalDateTime.now());
        videoReviewMapper.insert(record);

        // ③ 删除 Redis 缓存。四个动作一个都不能漏：
        //    不删则审核通过后用户最长 7 天仍看到「审核中」，下架后最长 7 天仍能播放。
        //    video:stat: 存的是计数字段，不受 status 影响，无需处理。
        redisTemplate.delete(RedisKey.VIDEO_INFO_BASE_PREFIX + videoId);

        log.info("治理动作完成：videoId={}, action={}({}), {} → {}, adminId={}",
                videoId, action.getCode(), action.getDesc(),
                current, target, AdminContext.getAdminId());
    }

    @Override
    public List<ReviewRecordVO> listReviews(Long videoId) {
        return videoReviewMapper.selectByVideoId(videoId).stream().map(r -> {
            ReviewRecordVO vo = new ReviewRecordVO();
            BeanUtils.copyProperties(r, vo);
            vo.setActionDesc(VideoReviewActionEnum.fromCode(r.getAction()).getDesc());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量换取用户昵称
     * Feign 失败时降级为空映射（前端显示「未知用户」），
     * 不让一个辅助信息拖垮整个列表接口
     */
    private Map<Long, String> loadNicknames(List<Long> userIds) {
        List<Long> distinct = userIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserBriefVO> users = userFeignClient.getProfilesByIds(distinct).getData();
            Map<Long, String> map = new HashMap<>();
            if (users != null) {
                for (UserBriefVO u : users) {
                    if (u.getId() != null) {
                        map.put(u.getId(), u.getNickname());
                    }
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("批量查询上传者昵称失败，降级为未知用户：{}", e.getMessage());
            return Map.of();
        }
    }
}
