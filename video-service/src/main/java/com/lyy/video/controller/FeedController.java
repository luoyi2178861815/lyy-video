package com.lyy.video.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.video.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端 - 关注动态
 * 网关路由：/api/video/feed → StripPrefix=1 → /video/feed
 */
@Tag(name = "C端 - 关注动态")
@RestController
@RequestMapping("/video/feed")
@Slf4j
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "关注动态流（我关注的作者 + 我自己的投稿，按发布时间倒序）")
    @GetMapping
    public Result<PageResult> feed(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 查询关注动态，第{}页，每页{}", userId, pageNum, pageSize);
        return Result.success(feedService.getFeed(userId, pageNum, pageSize));
    }
}
