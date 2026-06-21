package com.lyy.common.config;

import com.lyy.common.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 从网关转发的请求头 X-User-Id 中提取当前用户 ID，存入 BaseContext
 * 请求结束后自动清理 ThreadLocal，防止内存泄漏
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                BaseContext.setCurrentId(userId);
            } catch (NumberFormatException e) {
            }
        }
        return true; // 即使没有用户 ID 也放行（部分接口可能是公开的）
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        BaseContext.removeCurrentId();
    }
}
