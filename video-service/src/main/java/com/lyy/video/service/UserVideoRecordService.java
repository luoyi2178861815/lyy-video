package com.lyy.video.service;

public interface UserVideoRecordService {

    Integer getProgress(Long videoId);

    void playVideo(Long videoId);

    void submitProgress(Long videoId);
}
