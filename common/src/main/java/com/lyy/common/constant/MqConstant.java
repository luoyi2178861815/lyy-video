package com.lyy.common.constant;

public class MqConstant {
    //    评论
    public static final String VIDEO_COMMENT_EXCHANGE = "video.comment.exchange";
    public static final String VIDEO_COMMENT_QUEUE = "video.comment.queue";
    public static final String VIDEO_COMMENT_ROUTING_KEY = "video.comment.increment";
    //点赞
    public static final String VIDEO_LIKE_QUEUE = "video.like.queue";
    public static final String VIDEO_LIKE_EXCHANGE = "video.like.exchange";
    public static final String VIDEO_LIKE_ROUTING_KEY = "video.like.increment";
    //用户

    public static final String USER_LIKE_EXCHANGE = "user.like.exchange";
    public static final String USER_LIKE_QUEUE = "user.like.queue";
    public static final String USER_LIKE_ROUTING_KEY = "user.like.increment";

    //收藏
    public static final String VIDEO_COLLECT_EXCHANGE = "video.collect.exchange";
    public static final String VIDEO_COLLECT_QUEUE = "video.collect.queue";
    public static final String VIDEO_COLLECT_ROUTING_KEY = "video.collect.increment";
    //关注
    public static final String USER_FOLLOW_EXCHANGE = "user.follow.exchange";
    public static final String USER_FOLLOW_QUEUE = "user.follow.queue";
    public static final String USER_FOLLOW_ROUTING_KEY = "user.follow.increment";
    //    经验
    public static final String EXP_EXCHANGE = "exp.direct";
    public static final String EXP_QUEUE = "exp.queue";
    public static final String EXP_ROUTING_KEY = "exp.add";
    //投币
    public static final String VIDEO_COIN_EXCHANGE = "video.coin.exchange";
    public static final String VIDEO_COIN_QUEUE = "video.coin.queue";
    public static final String USER_COIN_QUEUE = "user.coin.queue";
    public static final String VIDEO_COIN_ROUTING_KEY = "video.coin.increment";
}
