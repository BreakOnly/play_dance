package com.dance.studio.config;

import com.dance.studio.entity.Admin;
import com.dance.studio.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时检查初始管理员密码：
 * 若仍为 schema.sql 中的占位 hash，则重置为 admin123（BCrypt），便于首次登录。
 * 生产环境请尽快通过修改密码接口/脚本更换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPasswordInitializer implements CommandLineRunner {

    private final AdminMapper adminMapper;
    private static final String PLACEHOLDER = "$2a$10$PLACEHOLDER_REPLACE_WITH_BCRYPT_HASH";
    private static final String DEFAULT_PWD = "admin123";

    @Override
    public void run(String... args) {
        Admin admin = adminMapper.findByUsername("admin");
        if (admin != null && PLACEHOLDER.equals(admin.getPasswordHash())) {
            String hash = new BCryptPasswordEncoder().encode(DEFAULT_PWD);
            adminMapper.updatePassword(admin.getId(), hash);
            log.warn("===== 已将管理员 admin 初始密码重置为: {} （请尽快修改）=====", DEFAULT_PWD);
        }
    }
}
