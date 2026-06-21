package com.lyy.recommend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 推荐服务启动类（微服务）
 * 功能：个性化推荐算法、热门排行
 */
@SpringBootApplication
@EnableFeignClients             // 启用 OpenFeign 远程调用
public class RecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendApplication.class, args);
    }
}
