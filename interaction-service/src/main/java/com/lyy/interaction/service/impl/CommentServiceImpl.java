package com.lyy.interaction.service.impl;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.context.BaseContext;
import com.lyy.common.dto.CommentIncrementMessage;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.result.Result;
import com.lyy.interaction.entity.dto.CommentAddDTO;
import com.lyy.interaction.entity.po.Comment;
import com.lyy.interaction.entity.po.CommentLike;
import com.lyy.interaction.entity.vo.CommentTreesVO;
import com.lyy.interaction.entity.vo.CommentUserVO;
import com.lyy.interaction.entity.vo.CommentVO;
import com.lyy.interaction.feign.UserFeignClient;
import com.lyy.interaction.mapper.CommentMapper;
import com.lyy.interaction.service.CommentService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserFeignClient userFeignClient;

    // ==================== 添加评论 ====================

    @Transactional
    public void addComment(CommentAddDTO commentAddDTO) {
        Long currentId = BaseContext.getCurrentId();
        if (commentAddDTO.getParentId() == null) {
            throw new BusinessException("父评论ID不能为空");
        }

        Comment comment = new Comment();
        comment.setVideoId(commentAddDTO.getVideoId());
        comment.setUserId(currentId);
        comment.setParentId(commentAddDTO.getParentId());
        comment.setReplyToUserId(commentAddDTO.getReplyToUserId());
        comment.setContent(commentAddDTO.getContent());
        comment.setLikeCount(0);
        comment.setStatus(2); // 审核中
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        if (commentAddDTO.getParentId() == 0) {
            // 根评论：先插入获取自增ID，再回写 root_id = id
            comment.setRootId(0L);
            commentMapper.insert(comment);
            commentMapper.updateRootId(comment.getId(), comment.getId());
        } else {
            // 子回复：查询父评论获取 rootId
            Comment parentComment = commentMapper.selectByCommentId(commentAddDTO.getParentId());
            if (parentComment == null) {
                throw new BusinessException("父评论不存在");
            }
            comment.setRootId(parentComment.getRootId());
            commentMapper.insert(comment);
        }

        CommentIncrementMessage message = new CommentIncrementMessage(commentAddDTO.getVideoId(), 1);
        rabbitTemplate.convertAndSend(MqConstant.VIDEO_COMMENT_EXCHANGE, MqConstant.VIDEO_COMMENT_ROUTING_KEY, message);
    }

    // ==================== 查询评论树 ====================

    public CommentTreesVO getCommentTree(Long videoId, int pageNum, int pageSize, Long currentUserId) {
        // 1. 分页查根评论
        int offset = (pageNum - 1) * pageSize;
        List<Comment> rootComments = commentMapper.selectRootPage(videoId, offset, pageSize);
        int total = commentMapper.selectRootCount(videoId);

        if (rootComments.isEmpty()) {
            CommentTreesVO empty = new CommentTreesVO();
            empty.setVideoId(videoId);
            empty.setTotal(total);
            empty.setCommentTrees(Collections.emptyList());
            return empty;
        }

        // 2. 查所有根评论的子回复
        List<Long> rootIds = rootComments.stream().map(Comment::getId).collect(Collectors.toList());
        List<Comment> allReplies = commentMapper.selectByRootIds(rootIds);

        // 3. 合并所有评论，收集需要查的用户ID
        List<Comment> allComments = new ArrayList<>(rootComments);
        allComments.addAll(allReplies);

        Set<Long> userIds = new HashSet<>();
        for (Comment c : allComments) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null && c.getReplyToUserId() > 0) {
                userIds.add(c.getReplyToUserId());
            }
        }

        // 4. Feign 批量查用户信息
        Map<Long, CommentUserVO> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            try {
                Result<List<CommentUserVO>> result = userFeignClient.getProfilesByIds(new ArrayList<>(userIds));
                if (result != null && result.getData() != null) {
                    userMap = result.getData().stream()
                            .collect(Collectors.toMap(CommentUserVO::getId, u -> u, (a, b) -> a));
                }
            } catch (Exception e) {
                // Feign 调用失败时降级：不显示用户信息，不中断主流程
            }
        }

        // 5. 查当前用户的点赞记录
        Set<Long> likedCommentIds = Collections.emptySet();
        if (currentUserId != null && currentUserId > 0) {
            likedCommentIds = allComments.stream()
                    .filter(c -> commentMapper.selectLikeExists(c.getId(), currentUserId) > 0)
                    .map(Comment::getId)
                    .collect(Collectors.toSet());
        }

        // 6. 转换为 CommentVO
        Map<Long, CommentVO> voMap = new HashMap<>();
        for (Comment c : allComments) {
            CommentVO vo = toCommentVO(c, userMap, likedCommentIds);
            voMap.put(vo.getId(), vo);
        }

        // 7. 构建树：先把根评论挑出来，再把子回复挂到对应父评论下
        List<CommentVO> rootVOs = new ArrayList<>();
        for (Comment c : rootComments) {
            CommentVO rootVO = voMap.get(c.getId());
            if (rootVO != null) {
                rootVOs.add(rootVO);
            }
        }
        for (Comment c : allReplies) {
            CommentVO childVO = voMap.get(c.getId());
            if (childVO == null) continue;
            CommentVO parentVO = voMap.get(childVO.getParentId());
            if (parentVO != null) {
                if (parentVO.getChildren() == null) {
                    parentVO.setChildren(new ArrayList<>());
                }
                parentVO.getChildren().add(childVO);
            }
        }

        CommentTreesVO result = new CommentTreesVO();
        result.setVideoId(videoId);
        result.setTotal(total);
        result.setCommentTrees(rootVOs);
        return result;
    }

    // ==================== 删除评论（软删除，仅作者） ====================

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectByCommentId(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (comment.getStatus() == 0) {
            throw new BusinessException("评论已被删除");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
        CommentIncrementMessage message = new CommentIncrementMessage(comment.getVideoId(), -1);
        rabbitTemplate.convertAndSend(MqConstant.VIDEO_COMMENT_EXCHANGE, MqConstant.VIDEO_COMMENT_ROUTING_KEY, message);
    }

    // ==================== 编辑评论（仅作者） ====================

    @Transactional
    public void editComment(Long commentId, Long userId, String content) {
        Comment comment = commentMapper.selectByCommentId(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (comment.getStatus() == 0) {
            throw new BusinessException("评论已被删除");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能编辑自己的评论");
        }
        commentMapper.updateContent(commentId, content);
    }

    // ==================== 点赞/取消点赞（toggle） ====================

    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectByCommentId(commentId);
        if (comment == null || comment.getStatus() == 0) {
            throw new BusinessException("评论不存在或已删除");
        }
        int exists = commentMapper.selectLikeExists(commentId, userId);
        if (exists > 0) {
            // 已点赞 → 取消
            commentMapper.deleteLike(commentId, userId);
            commentMapper.decrLikeCount(commentId);
            return false;
        } else {
            // 未点赞 → 点赞
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            commentMapper.insertLike(like);
            commentMapper.incrLikeCount(commentId);
            return true;
        }
    }

    // ==================== 工具方法 ====================

    private CommentVO toCommentVO(Comment c, Map<Long, CommentUserVO> userMap, Set<Long> likedIds) {
        CommentUserVO author = userMap.getOrDefault(c.getUserId(), new CommentUserVO());
        CommentUserVO replyToUser = userMap.getOrDefault(c.getReplyToUserId(), null);

        return CommentVO.builder()
                .id(c.getId())
                .videoId(c.getVideoId())
                .userId(c.getUserId())
                .username(author.getUsername())
                .nickname(author.getNickname() != null ? author.getNickname() : author.getUsername())
                .avatar(author.getAvatar())
                .parentId(c.getParentId())
                .rootId(c.getRootId())
                .replyToUserId(c.getReplyToUserId())
                .replyToUsername(replyToUser != null ? replyToUser.getUsername() : null)
                .replyToNickname(replyToUser != null
                        ? (replyToUser.getNickname() != null ? replyToUser.getNickname() : replyToUser.getUsername())
                        : null)
                .content(c.getContent())
                .likeCount(c.getLikeCount() != null ? c.getLikeCount() : 0)
                .liked(likedIds.contains(c.getId()))
                .status(c.getStatus())
                .createTime(c.getCreateTime())
                .children(new ArrayList<>())
                .build();
    }
}
