package com.lyy.video.mapper;

import com.lyy.video.entity.po.VideoReview;
import com.lyy.video.entity.vo.MyRejectedVideoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoReviewMapper {

    /** 插入一条治理流水 */
    void insert(VideoReview videoReview);

    /** 查询某视频的全部治理流水（倒序） */
    List<VideoReview> selectByVideoId(@Param("videoId") Long videoId);

    /** 查询某用户被驳回的视频及最新一次驳回原因 */
    List<MyRejectedVideoVO> selectMyRejected(@Param("userId") Long userId);
}
