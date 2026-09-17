package com.lyy.video.service;

import com.lyy.video.entity.vo.MyRejectedVideoVO;

import java.util.List;

/**
 * C 端审核区服务
 */
public interface ReviewService {

    /**
     * 查询指定用户被驳回的视频及原因
     * @param userId 用户ID
     * @return 视频列表，按上传时间倒序
     */
    List<MyRejectedVideoVO> getMyRejectedVideos(Long userId);
}
