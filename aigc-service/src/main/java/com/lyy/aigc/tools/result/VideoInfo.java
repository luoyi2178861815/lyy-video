package com.lyy.aigc.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoInfo {
    @JsonPropertyDescription("视频id")
    private String videoId;
    @JsonPropertyDescription("视频标题")
    private String title;
    @JsonPropertyDescription("视频作者")
    private String author;
    @JsonPropertyDescription("视频描述")
    private String description;
    @JsonPropertyDescription("视频时长")
    private String duration;
    @JsonPropertyDescription("视频播放地址")
    private String playUrl;
}
