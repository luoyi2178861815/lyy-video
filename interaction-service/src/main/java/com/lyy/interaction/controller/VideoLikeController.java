package com.lyy.interaction.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.interaction.service.VideoLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "视频点赞", description = "视频点赞/取消点赞/点赞列表")
@RestController
@Slf4j
@RequestMapping("/interact/like")
public class VideoLikeController {

    @Autowired
    private VideoLikeService videoLikeService;

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/{videoId}/like")
    public Result<Map<String, Object>> likeVideo(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} toggle点赞 视频 {}", userId, videoId);
        boolean liked = videoLikeService.likeVideo(videoId, userId);
        return Result.success(Map.of("liked", liked));
    }

    @Operation(summary = "查询用户点赞列表（分页，含视频及UP主信息）")
    @GetMapping("/list")
    public Result<PageResult> getUserLikeList(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 查询点赞列表，第{}页", userId, pageNum);
        PageResult page = videoLikeService.getUserLikePage(userId, pageNum, pageSize);
        return Result.success(page);
    }
    @Operation(summary = "查询该用户id是否点赞")
    @GetMapping("/isLike")
    public Result<Boolean> isLike(@RequestParam Long videoId,
                                  @RequestParam Long userId) {
        log.info("远程调用openfeign查询用户是否点赞....");
        return Result.success(videoLikeService.isLike(userId, videoId));
    }
}
