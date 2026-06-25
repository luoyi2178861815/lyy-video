package com.lyy.aigc.feign;

import com.lyy.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient("video-service")
public interface VideoFeignClient {

    /** 查询该视频是否存在（1存在，0不存在） */
    @GetMapping("/video/exists/{videoId}")
    Result<Integer> exists(@PathVariable Long videoId);

    /** 批量查询视频信息 */
    @GetMapping("/video/batch")
    Result<List<Map<String, Object>>> getVideosByIds(@RequestParam("ids") List<Long> ids);
}
