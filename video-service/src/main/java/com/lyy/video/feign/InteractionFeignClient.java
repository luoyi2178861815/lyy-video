package com.lyy.video.feign;

import com.lyy.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/*
* 调用interaction-service服务
* */
@FeignClient("interaction-service")
public interface InteractionFeignClient {
    @GetMapping("/interact/like/isLike")
    public Result<Boolean> isLike(@RequestParam Long videoId,
                                  @RequestParam Long userId);
    @GetMapping("/interact/collect/isCollect")
    public Result<Boolean> isCollect(@RequestParam Long videoId,
                                     @RequestParam Long userId);

    @GetMapping("/interact/coin/isCoined")
    public Result<Boolean> isCoined(@RequestParam Long videoId,
                                    @RequestParam Long userId);

    /**
     * 查「我关注的全部用户ID」，供动态流拼 IN 子句。刻意不分页，上限 1000 在 interaction 侧
     * @param userId 当前登录用户 ID，Feign 内部调用不经网关，必须显式传
     */
    @GetMapping("/interact/follow/following/ids")
    Result<List<Long>> getFollowingIds(@RequestParam Long userId);

    /**
     * 批量查「我在这批视频里赞过的 video_id」，供动态流点亮红心
     * @param videoIds 只传当前页的 id（≤ pageSize），不要传全部
     */
    @GetMapping("/interact/like/batchStatus")
    Result<List<Long>> getBatchLikeStatus(@RequestParam Long userId,
                                          @RequestParam List<Long> videoIds);
}
