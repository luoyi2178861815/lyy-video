package com.lyy.interaction.service;

import com.lyy.interaction.entity.dto.CommentAddDTO;
import com.lyy.interaction.entity.vo.CommentTreesVO;

public interface CommentService {
    void addComment(CommentAddDTO dto);
    CommentTreesVO getCommentTree(Long videoId, int pageNum, int pageSize, Long currentUserId);
    void deleteComment(Long commentId, Long userId);
    void editComment(Long commentId, Long userId, String content);
    boolean likeComment(Long commentId, Long userId); // true=已点赞, false=已取消
}
