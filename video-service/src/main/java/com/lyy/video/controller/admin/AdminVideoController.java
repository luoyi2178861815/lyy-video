package com.lyy.video.controller.admin;

import com.lyy.common.enums.VideoReviewActionEnum;
import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.video.entity.dto.ReviewActionDTO;
import com.lyy.video.entity.vo.AdminVideoDetailVO;
import com.lyy.video.entity.vo.ReviewRecordVO;
import com.lyy.video.service.admin.AdminVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B端 - 视频审核
 * 四个治理动作结构一致，统一委托 AdminVideoService.govern
 */
@Tag(name = "B端 - 视频审核")
@RestController
@RequestMapping("/admin/video")
@Slf4j
@RequiredArgsConstructor
public class AdminVideoController {

    private final AdminVideoService adminVideoService;

    @Operation(summary = "分页查询视频")
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        log.info("管理员查询视频列表：status={}, pageNum={}, pageSize={}", status, pageNum, pageSize);
        return Result.success(adminVideoService.pageVideos(status, pageNum, pageSize));
    }

    @Operation(summary = "视频详情（不限状态）")
    @GetMapping("/{id}")
    public Result<AdminVideoDetailVO> detail(@PathVariable("id") Long id) {
        log.info("管理员查看视频详情：id={}", id);
        return Result.success(adminVideoService.getDetail(id));
    }

    @Operation(summary = "审核通过：3 → 1")
    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable("id") Long id) {
        log.info("管理员审核通过：id={}", id);
        adminVideoService.govern(id, VideoReviewActionEnum.APPROVE, null);
        return Result.success("已通过");
    }

    @Operation(summary = "审核驳回：3 → 5")
    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable("id") Long id,
                                 @RequestBody ReviewActionDTO dto) {
        log.info("管理员驳回视频：id={}, reason={}", id, dto.getReason());
        adminVideoService.govern(id, VideoReviewActionEnum.REJECT, dto.getReason());
        return Result.success("已驳回");
    }

    @Operation(summary = "下架：1 → 2")
    @PutMapping("/{id}/offline")
    public Result<String> offline(@PathVariable("id") Long id,
                                  @RequestBody(required = false) ReviewActionDTO dto) {
        log.info("管理员下架视频：id={}", id);
        adminVideoService.govern(id, VideoReviewActionEnum.OFFLINE,
                dto == null ? null : dto.getReason());
        return Result.success("已下架");
    }

    @Operation(summary = "重新上架：2 → 1")
    @PutMapping("/{id}/online")
    public Result<String> online(@PathVariable("id") Long id) {
        log.info("管理员重新上架视频：id={}", id);
        adminVideoService.govern(id, VideoReviewActionEnum.ONLINE, null);
        return Result.success("已上架");
    }

    @Operation(summary = "查询该视频的治理流水")
    @GetMapping("/{id}/reviews")
    public Result<List<ReviewRecordVO>> reviews(@PathVariable("id") Long id) {
        return Result.success(adminVideoService.listReviews(id));
    }
}
