package com.lyy.video.utils;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;

/**
 * 视频处理工具类
 */
@Slf4j
public class VideoUtil {

    /**
     * 获取视频时长（秒）
     * @param videoPath 视频文件路径（本地路径或 OSS URL）
     * @return 时长（秒），失败返回 0
     */
    public static long getDurationSeconds(String videoPath) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();
            long durationMicroseconds = grabber.getLengthInTime();
            grabber.stop();
            
            long seconds = durationMicroseconds / 1_000_000;
            log.info("获取视频时长成功: {} 秒", seconds);
            return seconds;
        } catch (Exception e) {
            log.error("获取视频时长失败: {}", videoPath, e);
            return 0;
        }
    }
}
