package com.lyy.interaction.feign;

import com.lyy.common.result.Result;
import com.lyy.interaction.entity.vo.CommentUserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用 user-service 获取用户信息
 */
@FeignClient(name = "user-service")
public interface   UserFeignClient {

    @GetMapping("/user/profiles/batch")
    Result<List<CommentUserVO>> getProfilesByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/user/level/{userId}")
    Result<Integer> getUserLevel(@PathVariable("userId") Long userId);
}
