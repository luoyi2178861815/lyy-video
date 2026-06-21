package com.lyy.user;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 用户服务启动类（微服务）
 * 功能：注册、登录、个人信息管理
 */
@SpringBootApplication(scanBasePackages = {"com.lyy.user", "com.lyy.common"})
@EnableFeignClients
@EnableTransactionManagement// 启用事务管理
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
