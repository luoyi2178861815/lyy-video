package com.lyy.common.interceptor;

import com.lyy.common.config.UserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用 Web MVC 配置
 * 注册用户上下文拦截器，所有依赖 common 的微服务自动生效
 */
@Configuration
public class UserContextWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/app/user/login", "/app/user/register"); // 登录注册不需要拦截
    }
}
