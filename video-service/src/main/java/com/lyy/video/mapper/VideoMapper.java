package com.lyy.video.mapper;

import com.lyy.video.entity.po.Video;
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
}
