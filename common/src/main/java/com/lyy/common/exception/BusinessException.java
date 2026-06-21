package com.lyy.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * 抛出此异常会被全局异常处理器捕获，返回统一格式
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
