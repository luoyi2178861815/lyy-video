package com.lyy.user.controller.user;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.user.entity.dto.UserLoginDTO;
import com.lyy.user.entity.dto.UserPasswordDTO;
import com.lyy.user.entity.dto.UserUpdateDTO;
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.entity.po.User;
import com.lyy.user.entity.vo.UserLoginVO;
import com.lyy.user.entity.vo.UserProfileVO;
import com.lyy.user.ratelimit.RateLimit;
import com.lyy.user.ratelimit.RateLimitType;
import com.lyy.user.service.TokenFamilyService;
import com.lyy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "C端 - 前台用户", description = "给普通用户和UP主使用的接口")
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenFamilyService tokenFamilyService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        boolean success = userService.register(user);
        if (success) {
            return Result.success("注册成功");
        } else {
            return Result.error("注册失败");
        }
    }
    @RateLimit(key = "user:login", rate = 1, rateInterval = 1, limitType = RateLimitType.IP )
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO,
                                      HttpServletResponse response) {
        log.info("用户登录：{}", userLoginDTO.getUsername());
        User user = userService.login(userLoginDTO.getUsername(), userLoginDTO.getPassword());

        String accessToken = tokenFamilyService.issueAccessToken(user.getId());
        String refreshToken = tokenFamilyService.issueRefreshToken(user.getId());
        tokenFamilyService.setRefreshTokenCookie(response, refreshToken);

        UserLoginVO vo = UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .build();

        return Result.success(vo);
    }

    @PostMapping("/refresh")
    @Operation(summary = "无感刷新 Access Token")
    public Result<Map<String, String>> refresh(
            @CookieValue(value = "refresh_token", required = false) String rawRefreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Result.tokenExpired("未携带 Refresh Token");
        }

        return tokenFamilyService.refresh(rawRefreshToken, request, response);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<String> logout(HttpServletResponse response) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 退出登录", userId);
        tokenFamilyService.revokeAllFamily(userId);
        tokenFamilyService.clearRefreshTokenCookie(response);
        return Result.success("已退出登录");
    }

    @Operation(summary = "获取个人主页/UP主信息")
    @GetMapping("/profile/{userId}")
    public Result<UserProfileVO> getProfile(@PathVariable Long userId) {
        UserProfileVO vo = userService.getProfileById(userId);
        return Result.success(vo);
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<String> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 修改个人资料", userId);
        userService.updateProfile(userId, dto);
        return Result.success("修改成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> changePassword(@Valid @RequestBody UserPasswordDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 修改密码", userId);
        userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }

    @Operation(summary = "注销账号")
    @DeleteMapping("/account")
    public Result<String> deleteAccount() {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 注销账号", userId);
        userService.deleteAccount(userId);
        return Result.success("账号已注销");
    }

    @Operation(summary = "批量查询用户信息（供内部Feign调用）")
    @GetMapping("/profiles/batch")
    public Result<List<UserProfileVO>> getProfilesByIds(@RequestParam List<Long> ids) {
        List<UserProfileVO> list = userService.getProfilesByIds(ids);
        return Result.success(list);
    }
    @Operation(summary = "查询用户名称（供内部Feign调用）")
    @GetMapping("/profiles/{userId}")
    public Result<String> getUserByUsername(@PathVariable Long userId) {
        String authorName = userService.getAuthorNameByUserId(userId);
        return Result.success(authorName);
    }

    // ==================== 经验相关 ====================

    @Operation(summary = "每日分享上报")
    @PostMapping("/exp/share")
    public Result<String> shareVideo() {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 分享视频，获得经验", userId);
        userService.addExp(userId, 5, "daily_share");
        return Result.success("分享成功，经验+5");
    }

    @Operation(summary = "查询经验记录（最近30条）")
    @GetMapping("/exp/records")
    public Result<List<Map<String, Object>>> getExpRecords() {
        Long userId = BaseContext.getCurrentId();
        List<ExpRecord> records = userService.getExpRecords(userId);
        List<Map<String, Object>> list = records.stream().map(r -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("expValue", r.getExpValue());
            map.put("reason", r.getReason());
            map.put("createTime", r.getCreateTime());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return Result.success(list);
    }

    @Operation(summary = "查询用户等级和硬币数量（供内部Feign调用）")
    @GetMapping("/level/{userId}")
    public Result<Map<String, Object>> getUserLevel(@PathVariable Long userId) {
        Integer level = userService.getUserLevel(userId);
        Long coin = userService.getUserCoinCount(userId);
        return Result.success(Map.of("level", level, "coin", coin));
    }


    // ==================== 绑定相关 ====================

    @Operation(summary = "绑定邮箱（首次绑定获得经验）")
    @PutMapping("/bind/email")
    public Result<String> bindEmail(@RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return Result.error("邮箱不能为空");
        }
        // 检查是否首次绑定（当前 email 为空才算首次）
        UserProfileVO profile = userService.getProfileById(userId);
        boolean isFirstBind = (profile.getEmail() == null || profile.getEmail().isEmpty());

        // 更新邮箱
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail(email);
        userService.updateProfile(userId, dto);

        // 首次绑定发放经验
        if (isFirstBind) {
            userService.addExp(userId, 20, "bind_email");
            log.info("用户 {} 首次绑定邮箱，经验+20", userId);
        }
        return Result.success("绑定成功");
    }

    @Operation(summary = "绑定手机号（首次绑定获得经验）")
    @PutMapping("/bind/phone")
    public Result<String> bindPhone(@RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return Result.error("手机号不能为空");
        }
        // 检查是否首次绑定
        UserProfileVO profile = userService.getProfileById(userId);
        boolean isFirstBind = (profile.getPhone() == null || profile.getPhone().isEmpty());

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setPhone(phone);
        userService.updateProfile(userId, dto);

        if (isFirstBind) {
            userService.addExp(userId, 20, "bind_phone");
            log.info("用户 {} 首次绑定手机号，经验+20", userId);
        }
        return Result.success("绑定成功");
    }

    @Operation(summary = "实名认证（首次认证获得经验）")
    @PostMapping("/verify/real-name")
    public Result<String> verifyRealName(@RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        UserProfileVO profile = userService.getProfileById(userId);
        if (profile.getRealNameVerified() != null && profile.getRealNameVerified() == 1) {
            return Result.error("已实名认证，无需重复认证");
        }
        // 标记为已认证
        userService.updateRealNameVerified(userId);

        userService.addExp(userId, 50, "real_name");
        log.info("用户 {} 首次实名认证，经验+50", userId);
        return Result.success("实名认证成功");
    }

}
