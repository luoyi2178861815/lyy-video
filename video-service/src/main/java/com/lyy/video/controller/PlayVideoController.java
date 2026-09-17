package com.lyy.video.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.enums.PartitionEnum;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.common.utils.FileUploadUtil;
import com.lyy.video.entity.po.Video;
import com.lyy.video.entity.vo.MyVideoVO;
import com.lyy.video.entity.vo.VideoInfoVO;
import com.lyy.video.service.UserVideoRecordService;
import com.lyy.video.service.VideoService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/video")
@Slf4j
public class PlayVideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserVideoRecordService UserVideorecordService;
    @Autowired
    private FileUploadUtil fileUploadUtil;
    /**
     * 用户点击视频页面，获取视频信息
     */
    @GetMapping("/{videoId}")
    public Result<VideoInfoVO> getVideoInfo(@PathVariable("videoId") Long videoId) {
        log.info("用户获取视频信息videoId：{}", videoId);
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
    public Result<String> submitProgress(@PathVariable Long videoId,
                                         @RequestParam(required = false) Integer time) {
        log.info("用户上报播放心跳：videoId={}, time={}", videoId, time);
        UserVideorecordService.submitProgress(videoId, time);
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
     * 视频搜索（标题 + 简介模糊匹配）
     */
    @GetMapping("/search")
    public Result<PageResult> searchVideos(@RequestParam String keyword,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "12") int pageSize) {
        log.info("视频搜索：keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);
        PageResult result = videoService.searchVideos(keyword, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 视频分页列表（首页 / 分区筛选 / 排序）
     */
    @GetMapping("/page")
    public Result<PageResult> pageVideos(@RequestParam(required = false) Integer partition,
                                         @RequestParam(value = "page", defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "15") int pageSize) {
        log.info("视频分页查询：partition={}, pageNum={}, pageSize={}", partition, pageNum, pageSize);
        PageResult result = videoService.pageVideos(partition, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 视频/封面文件流（支持 Range 分段请求）
     */
    @GetMapping("/file/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(fileUploadUtil.getStoragePath()).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
            
            String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量查询视频信息（供内部Feign调用）
     */
    @GetMapping("/batch")
    public Result<List<Video>> getVideosByIds(@RequestParam List<Long> ids) {
        List<Video> list = videoService.getVideosByIds(ids);
        return Result.success(list);
    }

    /**
     * 获取全部分区列表
     */
    @GetMapping("/partitions")
    public Result<List<Map<String, Object>>> getPartitions() {
        List<Map<String, Object>> list = Arrays.stream(PartitionEnum.values())
                .map(p -> Map.of("code", (Object) p.getCode(), "name", (Object) p.getDesc()))
                .collect(Collectors.toList());
        return Result.success(list);
    }
    @GetMapping("/myVideos")
    public Result<List<MyVideoVO>> getMyVideos(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "12") int pageSize) {
        log.info("获取用户视频列表：pageNum={}, pageSize={}", pageNum, pageSize);
        List<MyVideoVO> result = videoService.getMyVideos(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取用户观看历史列表
     */
    @GetMapping("/history")
    public Result<PageResult> listWatchHistory(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "12") int pageSize) {
        log.info("获取观看历史：pageNum={}, pageSize={}", pageNum, pageSize);
        PageResult result = UserVideorecordService.listWatchHistory(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 删除单条观看历史
     */
    @DeleteMapping("/history/{videoId}")
    public Result<String> deleteWatchHistory(@PathVariable Long videoId) {
        log.info("删除观看历史：videoId={}", videoId);
        UserVideorecordService.deleteWatchHistory(videoId);
        return Result.success("删除成功");
    }
}
