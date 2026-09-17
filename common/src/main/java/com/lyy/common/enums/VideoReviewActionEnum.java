package com.lyy.common.enums;

import lombok.Getter;

/**
 * 视频治理动作
 * 与 video_review.action 字段取值一一对应
 */
@Getter
public enum VideoReviewActionEnum {

    APPROVE(1, "通过"),
    REJECT(2, "驳回"),
    OFFLINE(3, "下架"),
    ONLINE(4, "重新上架");

    private final int code;
    private final String desc;

    VideoReviewActionEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 按 code 取枚举，找不到抛异常（数据异常应尽早暴露） */
    public static VideoReviewActionEnum fromCode(int code) {
        for (VideoReviewActionEnum a : values()) {
            if (a.code == code) {
                return a;
            }
        }
        throw new IllegalArgumentException("无效的治理动作 code: " + code);
    }
}
