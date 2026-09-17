package com.lyy.video.service.admin;

import com.lyy.common.enums.VideoReviewActionEnum;
import com.lyy.common.result.PageResult;
import com.lyy.video.entity.vo.AdminVideoDetailVO;
import com.lyy.video.entity.vo.ReviewRecordVO;

import java.util.List;

/**
 * 管理端视频服务
 * 分页 / 详情 / 治理动作，与 C 端 VideoService 职责分离
 */
public interface AdminVideoService {

    /**
     * 分页查询视频
     * @param status   状态筛选，为 null 时默认查「审核中」
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果，records 为 List&lt;AdminVideoVO&gt;
     */
    PageResult pageVideos(Integer status, int pageNum, int pageSize);

    /**
     * 视频详情，不限制状态（已驳回/已下架/私密的管理员都要能看到）
     * @param videoId 视频ID
     * @return 详情
     */
    AdminVideoDetailVO getDetail(Long videoId);

    /**
     * 执行内容治理动作
     * @param videoId 视频ID
     * @param action  动作：通过/驳回/下架/重新上架
     * @param reason  原因，驳回必填，下架可选，其余忽略
     */
    void govern(Long videoId, VideoReviewActionEnum action, String reason);

    /**
     * 查询某视频的全部治理流水
     * @param videoId 视频ID
     * @return 流水列表，倒序
     */
    List<ReviewRecordVO> listReviews(Long videoId);
}
