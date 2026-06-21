package com.lyy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类（端口 8000）
 * 统一入口：路由转发、跨域处理、鉴权过滤
 */
@SpringBootApplication(scanBasePackages = {
        "com.lyy.gateway",
        "com.lyy.common.utils",
        "com.lyy.common.properties"
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
