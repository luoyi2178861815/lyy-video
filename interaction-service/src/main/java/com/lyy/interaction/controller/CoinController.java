package com.lyy.interaction.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.interaction.service.VideoCoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "投币", description = "给视频投币")
@RestController
@RequestMapping("/interact/coin")
@Slf4j
public class CoinController {

    @Autowired
    private VideoCoinService videoCoinService;


    @Operation(summary = "给视频投币")
    @PostMapping("/{videoId}")
    public Result<String> giveCoin(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 请求给视频 {} 投币", userId, videoId);
        
        // 调用 Service 层处理业务逻辑
        return videoCoinService.giveCoin(userId, videoId);
    }

    @Operation(summary = "查询用户是否已投币该视频")
    @GetMapping("/isCoined")
    public Result<Boolean> isCoined(@RequestParam Long videoId,
                                    @RequestParam Long userId) {
        log.info("远程调用openfeign该视频是否被用户投币...");
        boolean isCoined = videoCoinService.isCoinedByUser(userId, videoId);
        return Result.success(isCoined);
    }
}
