package com.lyy.video.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.video.entity.vo.MyRejectedVideoVO;
import com.lyy.video.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端 - 审核区
 * 作者查看自己被驳回的视频及原因
 */
@Tag(name = "C端 - 审核区")
@RestController
@RequestMapping("/video/review")
@Slf4j
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "查询我被驳回的视频及原因")
    @GetMapping("/myRejected")
    public Result<List<MyRejectedVideoVO>> myRejected() {
        Long userId = BaseContext.getCurrentId();
        log.info("用户查询被驳回视频：userId={}", userId);
        return Result.success(reviewService.getMyRejectedVideos(userId));
    }
}
