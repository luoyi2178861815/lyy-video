package com.lyy.user.init;

import com.lyy.user.entity.po.Admin;
import com.lyy.user.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 默认管理员播种器
 * admin 表为空时写入一个默认超管。仅首次启动生效，之后启动直接跳过。
 * 若不需要默认账号，删除本类即可（不影响其他逻辑）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_NAME = "超级管理员";

    private final AdminMapper adminMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        if (adminMapper.countAll() > 0) {
            return;
        }
        Admin admin = new Admin();
        admin.setUsername(DEFAULT_USERNAME);
        admin.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setName(DEFAULT_NAME);
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        adminMapper.insert(admin);
        log.warn("管理员表为空，已初始化默认超管：{} / {}，请尽快修改密码",
                DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
}
