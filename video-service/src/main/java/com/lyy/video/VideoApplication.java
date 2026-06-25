package com.lyy.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 视频服务启动类（微服务）
 * 功能：视频列表、视频详情、搜索
 */
@SpringBootApplication(scanBasePackages = {"com.lyy.video","com.lyy.common"})
@EnableFeignClients
@EnableTransactionManagement// 启用事务管理
@EnableAsync
public class VideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoApplication.class, args);
    }
}
