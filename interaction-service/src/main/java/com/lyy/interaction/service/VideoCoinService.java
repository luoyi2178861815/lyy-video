package com.lyy.interaction.service;

import com.lyy.common.result.Result;

public interface VideoCoinService {

    /**
     * 投币（包含用户等级校验、投币记录、经验发放）
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 投币结果
     */
    Result<String> giveCoin(Long userId, Long videoId);

    /** 判断用户是否已投币该视频 */
    boolean isCoinedByUser(Long userId, Long videoId);
}
