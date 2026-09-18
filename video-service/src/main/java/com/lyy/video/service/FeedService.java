package com.lyy.video.service;

import com.lyy.common.result.PageResult;

/**
 * C 端 - 关注动态流
 */
public interface FeedService {

    /**
     * 分页查询「我关注的作者 + 我自己」的已发布投稿，按发布时间倒序
     *
     * @param userId   当前登录用户
     * @param pageNum  页码，从 1 起
     * @param pageSize 每页条数，内部会截断到 1~20
     * @return PageResult{total, records:List&lt;FeedItemVO&gt;}
     */
    PageResult getFeed(Long userId, int pageNum, int pageSize);
}
