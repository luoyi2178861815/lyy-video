package com.lyy.video.mapper;

import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.MyVideoVO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface VideoMapper {
    void insert(Video video);

    Video getVideoInfo(@Param("videoId") Long videoId);

    void updatePlayCount(@Param("videoId") Long videoId);

    void updateCommentCount(@Param("videoId") Long videoId, @Param("increment") Integer increment);

    void updateLikeCount(@Param("videoId") Long videoId, @Param("increment") Integer increment);

    void updateCollectCount(@Param("videoId") Long videoId, @Param("increment") Integer increment);

    void updateCoinCount(@Param("videoId") Long videoId, @Param("increment") Integer increment);

    List<Video> selectByIds(@Param("ids") List<Long> ids);

    Integer exists(@Param("videoId") Long videoId);

    List<Video> searchVideos(@Param("keyword") String keyword,
                             @Param("offset") int offset,
                             @Param("pageSize") int pageSize);

    long countSearchVideos(@Param("keyword") String keyword);

    List<Video> pageVideos(@Param("partitionCode") Integer partitionCode,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);

    long countPageVideos(@Param("partitionCode") Integer partitionCode);

    List<Video> getMyVideos(@Param("offset") int offset,
                                @Param("pageSize") int pageSize,
                                @Param("userId") Long userId);

    /**
     * 管理端：按状态分页查询视频
     * @param status 状态筛选，为 null 时查全部
     */
    List<Video> pageVideosForAdmin(@Param("status") Integer status,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    /** 管理端：按状态统计视频总数 */
    long countVideosForAdmin(@Param("status") Integer status);

    /** 更新视频状态（治理动作用） */
    int updateStatus(@Param("videoId") Long videoId, @Param("status") Integer status);

    /**
     * 动态流：按作者集合分页查询已发布视频
     * 这是全链路里唯一承担排序与分页的地方。
     * 排序为 create_time DESC, id DESC——create_time 是秒级，必须用 id 兜底成全序，
     * 否则并列行在 LIMIT 翻页时次序不定，会出现卡片跨页重复或漏条
     * @param authorIds 作者 ID 集合（我关注的 ∪ 我自己），由调用方保证非空
     * @param offset    偏移量，(pageNum-1)*pageSize
     * @param pageSize  每页条数
     */
    List<Video> selectFeedPage(@Param("authorIds") List<Long> authorIds,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    /**
     * 动态流：统计同一条件的总数（用于前端判断「没有更多了」）
     */
    long countFeed(@Param("authorIds") List<Long> authorIds);
}
