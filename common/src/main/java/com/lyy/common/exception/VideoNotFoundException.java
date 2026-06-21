package com.lyy.common.exception;

import lombok.Getter;

/**
 * 自定义视频不存在异常
 * 抛出此异常会被全局异常处理器捕获，返回统一格式
 */
@Getter
public class VideoNotFoundException extends RuntimeException {
    private final Integer code;

    public VideoNotFoundException(String message) {
        super(message);
        this.code = 500;
    }

    public VideoNotFoundException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
