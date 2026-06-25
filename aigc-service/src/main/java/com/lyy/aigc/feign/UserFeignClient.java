package com.lyy.aigc.feign;

import com.lyy.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/user/profiles/{userId}")
    public Result<String> getUserByUsername(@PathVariable Long userId);



}
