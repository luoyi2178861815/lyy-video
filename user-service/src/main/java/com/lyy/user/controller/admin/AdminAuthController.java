package com.lyy.user.controller.admin;

import com.lyy.common.result.Result;
import com.lyy.user.entity.dto.AdminLoginDTO;
import com.lyy.user.entity.vo.AdminLoginVO;
import com.lyy.user.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端登录接口
 * 网关对该路径有白名单（登录时还没有 Token）
 */
@Tag(name = "B端 - 管理员登录")
@RestController
@RequestMapping("/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        log.info("管理员登录请求：username={}", dto.getUsername());
        return Result.success(adminService.login(dto));
    }
}
