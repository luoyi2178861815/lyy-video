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
}
