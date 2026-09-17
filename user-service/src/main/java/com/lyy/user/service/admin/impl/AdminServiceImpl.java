package com.lyy.user.service.admin.impl;

import com.lyy.common.constant.JwtClaimsConstant;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.utils.JwtUtil;
import com.lyy.user.entity.dto.AdminLoginDTO;
import com.lyy.user.entity.po.Admin;
import com.lyy.user.entity.vo.AdminLoginVO;
import com.lyy.user.mapper.AdminMapper;
import com.lyy.user.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端账号服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    /** BCrypt 编解码器，无状态可复用 */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        Admin admin = adminMapper.selectByUsername(dto.getUsername());
        // 账号不存在与密码错误返回同一提示，不泄露账号是否存在
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(403, "该管理员账号已被禁用");
        }

        // 姓名与账号写入 Token claims，网关解出后注入 X-Admin-Id / X-Admin-Name，
        // 下游服务零 Feign 即可拿到审核人信息
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, admin.getId());
        claims.put(JwtClaimsConstant.USERNAME, admin.getUsername());
        claims.put(JwtClaimsConstant.NAME, admin.getName());

        String token = jwtUtil.generateAdminToken(claims);
        log.info("管理员登录成功：id={}, username={}", admin.getId(), admin.getUsername());

        return AdminLoginVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .token(token)
                .build();
    }
}
