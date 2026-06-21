package com.lyy.interaction.service.impl;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.FollowMessage;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.interaction.entity.po.UserFollow;
import com.lyy.interaction.entity.vo.CommentUserVO;
import com.lyy.interaction.entity.vo.FollowUserVO;
import com.lyy.interaction.feign.UserFeignClient;
import com.lyy.interaction.mapper.UserFollowMapper;
import com.lyy.interaction.service.FollowService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowMapper followMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public boolean toggleFollow(Long userId, Long followeeId) {
        if (userId == null) {
            throw new BusinessException("用户不存在或者未登录");
        }
        if (followeeId == null) {
            throw new BusinessException("目标用户ID不能为空");
        }
        if (userId.equals(followeeId)) {
            throw new BusinessException("不能关注自己");
        }
        if (followMapper.exists(userId, followeeId) > 0) {
            followMapper.delete(userId, followeeId);
            sendFollowMessage(userId, followeeId, -1);
            return false;
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(userId);
            follow.setFolloweeId(followeeId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);
            sendFollowMessage(userId, followeeId, 1);
            return true;
        }
    }

    @Override
    public PageResult getFollowingList(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<UserFollow> follows = followMapper.selectFollowingPage(userId, offset, pageSize);
        int total = followMapper.selectFollowingCount(userId);

        if (follows.isEmpty()) {
            return new PageResult(total, Collections.emptyList());
        }

        List<Long> userIds = follows.stream()
                .map(UserFollow::getFolloweeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CommentUserVO> userMap = fetchUserMap(userIds);

        List<FollowUserVO> list = follows.stream().map(f -> {
            CommentUserVO u = userMap.get(f.getFolloweeId());
            return FollowUserVO.builder()
                    .userId(f.getFolloweeId())
                    .nickname(u != null ? u.getNickname() : null)
                    .avatar(u != null ? u.getAvatar() : null)
                    .followTime(f.getCreateTime())
                    .isMutual(false)
                    .build();
        }).collect(Collectors.toList());

        return new PageResult(total, list);
    }

    @Override
    public PageResult getFansList(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<UserFollow> fans = followMapper.selectFansPage(userId, offset, pageSize);
        int total = followMapper.selectFansCount(userId);

        if (fans.isEmpty()) {
            return new PageResult(total, Collections.emptyList());
        }

        List<Long> userIds = fans.stream()
                .map(UserFollow::getFollowerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CommentUserVO> userMap = fetchUserMap(userIds);

        List<FollowUserVO> list = fans.stream().map(f -> {
            CommentUserVO u = userMap.get(f.getFollowerId());
            return FollowUserVO.builder()
                    .userId(f.getFollowerId())
                    .nickname(u != null ? u.getNickname() : null)
                    .avatar(u != null ? u.getAvatar() : null)
                    .followTime(f.getCreateTime())
                    .isMutual(false)
                    .build();
        }).collect(Collectors.toList());

        return new PageResult(total, list);
    }

    @Override
    public boolean isFollowing(Long userId, Long followeeId) {
        return followMapper.exists(userId, followeeId) > 0;
    }

    private void sendFollowMessage(Long followerId, Long followeeId, int increment) {
        FollowMessage msg = new FollowMessage();
        msg.setFollowerId(followerId);
        msg.setFolloweeId(followeeId);
        msg.setIncrement(increment);
        rabbitTemplate.convertAndSend(MqConstant.USER_FOLLOW_EXCHANGE, MqConstant.USER_FOLLOW_ROUTING_KEY, msg);
    }

    private Map<Long, CommentUserVO> fetchUserMap(List<Long> userIds) {
        try {
            Result<List<CommentUserVO>> result = userFeignClient.getProfilesByIds(userIds);
            if (result != null && result.getData() != null) {
                return result.getData().stream()
                        .collect(Collectors.toMap(CommentUserVO::getId, v -> v, (a, b) -> a));
            }
        } catch (Exception e) {
            // Feign 降级：用户信息留空
        }
        return Collections.emptyMap();
    }
}
