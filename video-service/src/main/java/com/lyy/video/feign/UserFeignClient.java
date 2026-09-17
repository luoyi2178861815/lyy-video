package com.lyy.video.feign;

import com.lyy.common.result.Result;
import com.lyy.video.feign.vo.UserBriefVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/user/profiles/{userId}")
    public Result<String> getUserByUsername(@PathVariable Long userId);

    /**
     * 批量查询用户简要信息（供管理端列表显示上传者昵称）
     * @param ids 用户ID列表
     * @return 用户简要信息列表
     */
    @GetMapping("/user/profiles/batch")
    Result<List<UserBriefVO>> getProfilesByIds(@RequestParam("ids") List<Long> ids);
}
