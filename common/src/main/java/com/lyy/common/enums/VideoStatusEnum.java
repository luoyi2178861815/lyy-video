package com.lyy.common.enums;

import lombok.Getter;

@Getter
public enum VideoStatusEnum {

    PUBLISHED(1, "已发布"),
    REMOVED(2, "已下架"),
    REVIEWING(3, "审核中"),
    PRIVATE(4, "私密"),
    /** 审核不通过：仅作者本人与后台管理员可见 */
    REVIEW_REJECTED(5, "审核不通过");

    private final int code;
    private final String desc;

    VideoStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VideoStatusEnum fromCode(int code) {
        for (VideoStatusEnum s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的状态code: " + code);
    }
}