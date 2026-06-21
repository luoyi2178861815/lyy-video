package com.lyy.common.enums;

import lombok.Getter;

@Getter
public enum PartitionEnum {

    BANGUMI(1, "番剧"),
    GUOCHUANG(2, "国创"),
    VARIETY(3, "综艺"),
    ANIME(4, "动画"),
    GHOST(5, "鬼畜"),
    DANCE(6, "舞蹈"),
    ENTERTAINMENT(7, "娱乐"),
    TECH(8, "科技数码"),
    FOOD(9, "美食"),
    CAR(10, "汽车"),
    SPORTS(11, "体育运动"),
    VLOG(12, "vlog"),
    MOVIE(13, "电影"),
    TV_SERIES(14, "电视剧"),
    DOCUMENTARY(15, "纪录片"),
    GAME(16, "游戏"),
    MUSIC(17, "音乐"),
    FILM(18, "影视"),
    KNOWLEDGE(19, "知识"),
    INFO(20, "资讯"),
    MINI_DRAMA(21, "小剧场"),
    FASHION(22, "时尚美妆"),
    ANIMAL(23, "动物"),
    MORE(99, "更多");

    private final int code;
    private final String desc;

    PartitionEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PartitionEnum fromCode(int code) {
        for (PartitionEnum p : values()) {
            if (p.code == code) {
                return p;
            }
        }
        throw new IllegalArgumentException("无效的分区code: " + code);
    }
}