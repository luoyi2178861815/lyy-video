package com.lyy.interaction.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.interaction.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户关注", description = "关注/取关 / 关注列表 / 粉丝列表")
@RestController
@Slf4j
@RequestMapping("/interact/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Operation(summary = "关注/取关用户")
    @PostMapping("/{userId}/follow")
    public Result<Map<String, Object>> toggleFollow(@PathVariable Long userId) {
        Long currentUserId = BaseContext.getCurrentId();
        log.info("用户 {} toggle关注 用户 {}", currentUserId, userId);
        boolean followed = followService.toggleFollow(currentUserId, userId);
        return Result.success(Map.of("followed", followed));
    }

    @Operation(summary = "我的关注列表（分页）")
    @GetMapping("/following/list")
    public Result<PageResult> getFollowingList(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 查询关注列表，第{}页", userId, pageNum);
        PageResult page = followService.getFollowingList(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "我的粉丝列表（分页）")
    @GetMapping("/fans/list")
    public Result<PageResult> getFansList(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 查询粉丝列表，第{}页", userId, pageNum);
        PageResult page = followService.getFansList(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "是否已关注某用户")
    @GetMapping("/{userId}/status")
    public Result<Map<String, Object>> isFollowing(@PathVariable Long userId) {
        Long currentUserId = BaseContext.getCurrentId();
        boolean following = followService.isFollowing(currentUserId, userId);
        return Result.success(Map.of("following", following));
    }

    @Operation(summary = "查询我关注的全部用户ID（供动态流内部调用，刻意不分页）")
    @GetMapping("/following/ids")
    public Result<List<Long>> getFollowingIds(@RequestParam Long userId) {
        // 参数用 @RequestParam 显式传，不依赖 BaseContext：
        // Feign 内部调用不经网关，网关注入的 X-User-Id 不会被转发，BaseContext 在这里是空的。
        // 这与现有 InteractionFeignClient.isLike(videoId, userId) 的写法一致。
        log.info("查询用户 {} 的关注ID列表", userId);
        return Result.success(followService.getFollowingIds(userId));
    }
}
