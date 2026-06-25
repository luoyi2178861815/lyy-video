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
                            @Param("sort") String sort,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);

    long countPageVideos(@Param("partitionCode") Integer partitionCode);

    List<Video> getMyVideos(@Param("offset") int offset,
                                @Param("pageSize") int pageSize,
                                @Param("userId") Long userId);
}
