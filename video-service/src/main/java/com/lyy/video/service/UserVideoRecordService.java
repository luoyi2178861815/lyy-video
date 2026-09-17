package com.lyy.video.service;

import com.lyy.common.result.PageResult;

public interface UserVideoRecordService {

    Integer getProgress(Long videoId);

    void playVideo(Long videoId);

    void submitProgress(Long videoId, Integer time);

    PageResult listWatchHistory(int pageNum, int pageSize);

    void deleteWatchHistory(Long videoId);
}
