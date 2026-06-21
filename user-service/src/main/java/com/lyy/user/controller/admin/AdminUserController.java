package com.lyy.user.controller.admin;

import com.lyy.common.result.PageResult;
import com.lyy.common.result.Result;
import com.lyy.user.entity.dto.AdminUserAddDTO;
import com.lyy.user.entity.dto.AdminUserUpdateDTO;
import com.lyy.user.entity.vo.UserProfileVO;
import com.lyy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "B端 - 管理后台", description = "管理员管理用户的接口")
@RestController
@Slf4j
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Result<PageResult> getUserPage(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String keyword) {
        log.info("管理员查询用户列表：pageNum={}, pageSize={}, keyword={}", pageNum, pageSize, keyword);
        PageResult page = userService.getUserPage(pageNum, pageSize, keyword);
        return Result.success(page);
    }

    @Operation(summary = "查看用户详情")
    @GetMapping("/{id}")
    public Result<UserProfileVO> getUserById(@PathVariable Long id) {
        log.info("管理员查看用户：id={}", id);
        UserProfileVO vo = userService.getUserById(id);
        return Result.success(vo);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<String> addUser(@Valid @RequestBody AdminUserAddDTO dto) {
        log.info("管理员新增用户：username={}", dto.getUsername());
        userService.addUser(dto);
        return Result.success("新增成功");
    }

    @Operation(summary = "编辑用户")
    @PutMapping
    public Result<String> updateUser(@Valid @RequestBody AdminUserUpdateDTO dto) {
        log.info("管理员编辑用户：id={}", dto.getId());
        userService.updateUser(dto);
        return Result.success("编辑成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        log.info("管理员删除用户：id={}", id);
        userService.deleteUser(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "封禁/解封用户")
    @PutMapping("/{id}/status")
    public Result<String> changeUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("管理员修改用户状态：id={}, status={}", id, status);
        userService.changeUserStatus(id, status);
        return Result.success(status == 1 ? "已解封" : "已封禁");
    }
}
