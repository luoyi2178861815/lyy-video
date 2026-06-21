package com.lyy.common.enums;

import lombok.Getter;

@Getter
public enum ContentStatementEnum {

    NO_MARK(0, "内容无需标注"),
    AI_GENERATED(1, "含AI生成内容"),
    FICTIONAL(2, "含虚构演绎内容"),
    MARKETING(3, "内容含营销信息"),
    PERSONAL_VIEW(4, "个人观点，仅供参考"),
    REPOST(5, "内容为转载"),
    ORIGINAL_PROHIBIT(6, "内容为自制：未经作者允许，禁止转载");

    private final int code;
    private final String desc;

    ContentStatementEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ContentStatementEnum fromCode(int code) {
        for (ContentStatementEnum s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的内容声明code: " + code);
    }
}