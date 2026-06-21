package com.lyy.video.service;

import com.lyy.video.entity.dto.VideoUploadDTO;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.VideoInfoVO;

import java.util.List;


public interface VideoService {
    void uploadVideo(VideoUploadDTO videoUploadDTO);

    VideoInfoVO getVideoInfo(Long videoId);

    void incrementCommentCount(Long videoId, Integer increment);

    void incrementLikeCount(Long videoId, Integer increment);

    void incrementCollectCount(Long videoId, Integer increment);

    void incrementCoinCount(Long videoId, Integer increment);

    Integer exists(Long videoId);

    List<Video> getVideosByIds(List<Long> ids);
}

