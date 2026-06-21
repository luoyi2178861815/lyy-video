package com.lyy.interaction.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.interaction.entity.dto.CommentAddDTO;
import com.lyy.interaction.feign.UserFeignClient;
import com.lyy.interaction.entity.vo.CommentTreesVO;
import com.lyy.interaction.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "评论", description = "视频评论相关接口")
@RestController
@RequestMapping("/interact/comment")
@Slf4j
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Operation(summary = "添加评论")
    @PostMapping("/add")
    public Result<String> addComment(@Valid @RequestBody CommentAddDTO commentAddDTO) {
        log.info("用户添加评论：{}", commentAddDTO);
        // Lv0 用户禁止评论
        Long userId = BaseContext.getCurrentId();
        Result<Integer> levelResult = userFeignClient.getUserLevel(userId);
        if (levelResult != null && levelResult.getData() != null && levelResult.getData() == 0) {
            return Result.error("Lv0 用户暂不支持评论，请先获取经验升级");
        }
        commentService.addComment(commentAddDTO);
        log.info("添加评论成功！");
        return Result.success("添加评论成功！");
    }

    @Operation(summary = "获取视频评论树（分页）")
    @GetMapping("/tree")
    public Result<CommentTreesVO> getCommentTree(@RequestParam Long videoId,
                                                  @RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        log.info("获取视频 {} 评论树，第{}页", videoId, pageNum);
        Long currentUserId = null;
        try {
            currentUserId = BaseContext.getCurrentId();
        } catch (Exception ignored) {
            // 未登录用户也能看评论，只是不显示点赞状态
        }
        CommentTreesVO vo = commentService.getCommentTree(videoId, pageNum, pageSize, currentUserId);
        return Result.success(vo);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<String> deleteComment(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 删除评论 {}", userId, id);
        commentService.deleteComment(id, userId);
        return Result.success("删除成功");
    }

    @Operation(summary = "编辑评论内容")
    @PutMapping("/{id}")
    public Result<String> editComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        String content = body.get("content");
        log.info("用户 {} 编辑评论 {}", userId, id);
        commentService.editComment(id, userId, content);
        return Result.success("编辑成功");
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> likeComment(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} toggle点赞 评论 {}", userId, id);
        boolean liked = commentService.likeComment(id, userId);
        return Result.success(Map.of("liked", liked));
    }
}
