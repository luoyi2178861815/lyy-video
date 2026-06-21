package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.Comment;
import com.lyy.interaction.entity.po.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper {
    // ==================== 写入 ====================
    void insert(Comment comment);
    void updateRootId(@Param("id") Long id, @Param("rootId") Long rootId);
    int updateContent(@Param("id") Long id, @Param("content") String content);
    int deleteById(@Param("id") Long id);   // 软删除，status=0

    // ==================== 查询 ====================
    Comment selectByCommentId(@Param("id") Long id);
    List<Comment> selectAllByVideoId(@Param("videoId") Long videoId);
    List<Comment> selectRootPage(@Param("videoId") Long videoId,
                                 @Param("offset") int offset,
                                 @Param("pageSize") int pageSize);
    int selectRootCount(@Param("videoId") Long videoId);
    List<Comment> selectByRootIds(@Param("rootIds") List<Long> rootIds);

    // ==================== 点赞 ====================
    int insertLike(CommentLike like);
    int deleteLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
    int selectLikeExists(@Param("commentId") Long commentId, @Param("userId") Long userId);
    int incrLikeCount(@Param("id") Long id);
    int decrLikeCount(@Param("id") Long id);
}
