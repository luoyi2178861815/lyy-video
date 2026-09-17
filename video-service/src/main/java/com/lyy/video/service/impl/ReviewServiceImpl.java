package com.lyy.video.service.impl;

import com.lyy.video.entity.vo.MyRejectedVideoVO;
import com.lyy.video.mapper.VideoReviewMapper;
import com.lyy.video.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * C 端审核区服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final VideoReviewMapper videoReviewMapper;

    @Override
    public List<MyRejectedVideoVO> getMyRejectedVideos(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return videoReviewMapper.selectMyRejected(userId);
    }
}
