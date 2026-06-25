package com.lyy.video.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.common.utils.FileUploadUtil;
import com.lyy.video.entity.dto.VideoUploadDTO;
import com.lyy.video.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/video/file")
@Slf4j
public class FileController {

    @Autowired
    private FileUploadUtil fileUploadUtil;
    @Autowired
    private VideoService videoService;


    /**
     * 用户发布视频
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(VideoUploadDTO videoUploadDTO) {
        log.info("用户发布视频：{}", videoUploadDTO);
        MultipartFile file = videoUploadDTO.getVideoFile();
        MultipartFile coverFile = videoUploadDTO.getCoverFile();
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            String filePath = fileUploadUtil.saveFile(file);
            String coverPath = fileUploadUtil.saveFile(coverFile, true);
            log.info("文件上传成功，保存路径：file:{} coverFile:{}", filePath, coverPath);
            videoUploadDTO.setVideoUrl(filePath);
            videoUploadDTO.setCoverUrl(coverPath);
            videoService.uploadVideo(videoUploadDTO);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }


}