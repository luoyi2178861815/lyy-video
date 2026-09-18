package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.VideoLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoLikeMapper {

    /** 检查用户是否已点赞该视频 */
    int exits(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /** 取消点赞 */
    void deletelike(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /** 点赞 */
    void like(VideoLike videoLike);

    /** 分页查询用户点赞的视频ID列表 */
    List<VideoLike> selectUserLikePage(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    /** 用户点赞总数 */
    int selectUserLikeCount(@Param("userId") Long userId);

    /**
     * 批量查询：在这批视频里，我赞过哪些（返回 video_id 列表）
     * 走 DB 而不是 Redis：现有单条版 isLike 查的就是 DB，批量版走 Redis 会与单条版状态不一致
     */
    List<Long> selectLikedVideoIds(@Param("userId") Long userId,
                                   @Param("videoIds") List<Long> videoIds);
}
