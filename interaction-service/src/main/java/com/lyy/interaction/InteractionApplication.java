package com.lyy.interaction;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 互动服务启动类（微服务）
 * 功能：点赞、点踩、评论、收藏、投币
 */
@SpringBootApplication(scanBasePackages = {"com.lyy.interaction","com.lyy.common"})
@EnableFeignClients   // 启用 OpenFeign 远程调用
@EnableTransactionManagement
public class InteractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteractionApplication.class, args);
    }
}
