package com.lyy.video.feign;

import com.lyy.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}
