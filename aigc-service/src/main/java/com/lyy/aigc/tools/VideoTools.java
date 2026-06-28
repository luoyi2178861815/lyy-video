package com.lyy.aigc.tools;

import com.lyy.aigc.config.ToolResultHolder;
import com.lyy.aigc.constants.Constant;
import com.lyy.aigc.feign.UserFeignClient;
import com.lyy.aigc.feign.VideoFeignClient;
import com.lyy.aigc.tools.result.VideoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 视频工具
 */
@Slf4j
@Component
public class VideoTools {
    @Autowired
    private VideoFeignClient videoFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    @Tool(description = Constant.Tools.QUERY_VIDEO_BY_ID)
    public VideoInfo queryVideoById(@ToolParam(description = Constant.ToolParams.VIDEO_ID) Long videoId , ToolContext toolContext){
        log.info("大模型调用查询工具，queryVideoById: {}", videoId);
        try {
            return Optional.ofNullable(videoId)
                    .map(id -> {
                        Map<String, Object> video = videoFeignClient.getVideosByIds(List.of(id)).getData().get(0);
                        Number userId = (Number) video.get("userId");
                        String author = userFeignClient.getUserByUsername(userId.longValue()).getData();
                        video.put("author", author);
                        return video;
                    })
                    .map(stringObjectMap -> VideoInfo.builder()
                            .videoId(String.valueOf(stringObjectMap.get("id")))
                            .title((String) stringObjectMap.get("title"))
                            .author((String) stringObjectMap.get("author"))
                            .description((String) stringObjectMap.get("introduction"))
                            .duration(String.valueOf(stringObjectMap.get("duration")))
                            .playUrl((String) stringObjectMap.get("videoUrl"))
                            .build())
                    .map(videoInfo -> {
                       var requestId = toolContext.getContext().get(Constant.REQUEST_ID);
                        ToolResultHolder.put((String) requestId,"videoInfo_"+videoInfo.getVideoId(),videoInfo);
                        return videoInfo;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("查询视频失败, videoId: {}", videoId, e);
            return null;
        }
    }
}
