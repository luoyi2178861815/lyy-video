package com.lyy.video.controller;

import com.lyy.common.result.Result;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.VideoInfoVO;
import com.lyy.video.service.UserVideoRecordService;
import com.lyy.video.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/video")
@Slf4j
public class PlayVideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserVideoRecordService UserVideorecordService;
    /**
     * 用户点击视频页面，获取视频信息
     */
    @GetMapping("/{videoId}")
    public Result<VideoInfoVO> getVideoInfo(@PathVariable("videoId") Long videoId) {
        log.info("用户获取视频信息：{}", videoId);
        VideoInfoVO videoInfoVO = videoService.getVideoInfo(videoId);
        return Result.success(videoInfoVO);
    }
    /*
    * 用户点击并开始播放
    * */
    @PostMapping("/play/{videoId}")
    public Result<String> playVideo(@PathVariable Long videoId) {
        log.info("用户开始播放视频：{}", videoId);
        UserVideorecordService.playVideo(videoId);
        return Result.success("播放成功");
    }

    /**
     * 用户提交播放进度
     */
    @PostMapping("/progress/{videoId}")
    public Result<String> submitProgress(@PathVariable Long videoId) {
        log.info("用户上报播放心跳：videoId={}", videoId);
        UserVideorecordService.submitProgress(videoId);
        return Result.success("上报成功");
    }

    /**
     * 获取用户观看进度（断点续播）
     */
    @GetMapping("/progress/{videoId}")
    public Result<Integer> getProgress(@PathVariable Long videoId) {
        Integer lastWatchTime = UserVideorecordService.getProgress(videoId);
        return Result.success(lastWatchTime);
    }  
    /**
     * 查询该视频是否存在
     *  //1代表存在，0代表不存在
     */
    @GetMapping("/exists/{videoId}")
    public Result<Integer> exists(@PathVariable Long videoId) {
        //1代表存在，0代表不存在
        Integer exists = videoService.exists(videoId);
        return Result.success(exists);
    }

    /**
     * 批量查询视频信息（供内部Feign调用）
     */
    @GetMapping("/batch")
    public Result<List<Video>> getVideosByIds(@RequestParam List<Long> ids) {
        List<Video> list = videoService.getVideosByIds(ids);
        return Result.success(list);
    }
}
