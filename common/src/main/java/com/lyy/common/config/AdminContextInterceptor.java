package com.lyy.common.config;

import com.lyy.common.context.AdminContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端上下文拦截器
 * 从网关注入的 X-Admin-Id / X-Admin-Name 还原当前管理员，存入 AdminContext。
 * X-Admin-Name 在网关侧做了 URL 编码，此处解码还原中文姓名。
 */
@Slf4j
public class AdminContextInterceptor implements HandlerInterceptor {

    private static final String HEADER_ADMIN_ID = "X-Admin-Id";
    private static final String HEADER_ADMIN_NAME = "X-Admin-Name";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String idHeader = request.getHeader(HEADER_ADMIN_ID);
        if (idHeader != null && !idHeader.isEmpty()) {
            try {
                AdminContext.setAdminId(Long.parseLong(idHeader));
            } catch (NumberFormatException e) {
                log.warn("X-Admin-Id 非法：{}", idHeader);
            }
        }

        String nameHeader = request.getHeader(HEADER_ADMIN_NAME);
        if (nameHeader != null && !nameHeader.isEmpty()) {
            try {
                AdminContext.setAdminName(URLDecoder.decode(nameHeader, StandardCharsets.UTF_8));
            } catch (Exception e) {
                // 解码失败时退回原始值，保证审核人姓名不至于整体丢失
                AdminContext.setAdminName(nameHeader);
            }
        }
        return true; // 即使没有管理员信息也放行，鉴权由网关负责
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        AdminContext.remove();
    }
}
