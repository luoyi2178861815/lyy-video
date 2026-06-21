package com.lyy.interaction.controller;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.interaction.entity.dto.CollectAddDTO;
import com.lyy.interaction.entity.dto.FolderAddDTO;
import com.lyy.interaction.entity.dto.FolderUpdateDTO;
import com.lyy.interaction.entity.vo.CollectFolderDetailVO;
import com.lyy.interaction.entity.vo.CollectFolderVO;
import com.lyy.interaction.service.CollectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "视频收藏", description = "收藏夹CRUD / 收藏/取消收藏视频")
@RestController
@Slf4j
@RequestMapping("/interact/collect")
public class CollectController {

    @Autowired
    private CollectService collectService;

    @Operation(summary = "创建收藏夹")
    @PostMapping("/folder")
    public Result<CollectFolderVO> createFolder(@Valid @RequestBody FolderAddDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 创建收藏夹: {}", userId, dto.getName());
        CollectFolderVO vo = collectService.createFolder(userId, dto);
        return Result.success(vo);
    }

    @Operation(summary = "删除收藏夹")
    @DeleteMapping("/folder/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 删除收藏夹 {}", userId, id);
        collectService.deleteFolder(userId, id);
        return Result.success();
    }

    @Operation(summary = "编辑收藏夹")
    @PutMapping("/folder/{id}")
    public Result<Void> updateFolder(@PathVariable Long id, @Valid @RequestBody FolderUpdateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        dto.setId(id);
        log.info("用户 {} 编辑收藏夹 {}", userId, id);
        collectService.updateFolder(userId, dto);
        return Result.success();
    }

    @Operation(summary = "我的收藏夹列表")
    @GetMapping("/folder/list")
    public Result<List<CollectFolderVO>> getFolderList() {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 查询收藏夹列表", userId);
        List<CollectFolderVO> list = collectService.getFolderList(userId);
        return Result.success(list);
    }

    @Operation(summary = "查看收藏夹内视频（分页）")
    @GetMapping("/folder/{id}")
    public Result<CollectFolderDetailVO> getFolderDetail(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        log.info("查询收藏夹 {} 详情，第{}页", id, pageNum);
        CollectFolderDetailVO vo = collectService.getFolderDetail(id, pageNum, pageSize);
        return Result.success(vo);
    }

    @Operation(summary = "收藏视频")
    @PostMapping
    public Result<String> collectVideo(@Valid @RequestBody CollectAddDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 收藏视频 {}", userId, dto.getVideoId());
        collectService.collectVideo(userId, dto);
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{videoId}")
    public Result<String> uncollectVideo(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 取消收藏视频 {}", userId, videoId);
        collectService.uncollectVideo(userId, videoId);
        return Result.success("取消收藏成功");
    }
    @Operation(summary = "查询用户是否收藏该视频")
    @GetMapping("/isCollect")
    public Result<Boolean> isCollect(@RequestParam Long videoId,
                                     @RequestParam Long userId) {
        log.info("远程调用openfeign该视频是否被用户收藏...");
        boolean isCollect = collectService.isCollectedByUser(userId, videoId);
        return Result.success(isCollect);
    }
}
