package com.lyy.common.interceptor;

import com.lyy.common.config.AdminContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理端 Web MVC 配置
 * 只把管理端上下文拦截器注册到 /admin/** 上，C 端路径完全不受影响。
 */
@Configuration
public class AdminContextWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminContextInterceptor())
                .addPathPatterns("/admin/**");
    }
}
