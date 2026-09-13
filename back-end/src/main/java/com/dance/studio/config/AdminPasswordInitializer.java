package com.dance.studio.config;

import com.dance.studio.mapper.AdminMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AdminPasswordInitializer implements CommandLineRunner {
    private final AdminMapper mapper;
    private final Environment environment;

    @Value("${app.admin.initial-password:}")
    private String initialPassword;

    @Override
    public void run(String... args) {
        var a = mapper.findByUsername("admin");
        if (a == null) throw new IllegalStateException("请先初始化管理员档案");
        var encoder = new BCryptPasswordEncoder();
        boolean placeholder = a.getPasswordHash().contains("PLACEHOLDER");
        boolean insecure = !placeholder && encoder.matches("admin123", a.getPasswordHash());
        if ((placeholder || insecure) && !initialPassword.isBlank()) {
            if (initialPassword.length() < 12
                    || initialPassword.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalStateException("ADMIN_INITIAL_PASSWORD 至少 12 个字符且不超过 72 字节");
            mapper.updatePassword(a.getId(), encoder.encode(initialPassword));
        } else if (placeholder
                || (insecure && !environment.acceptsProfiles(Profiles.of("local", "test"))))
            throw new IllegalStateException("请通过 ADMIN_INITIAL_PASSWORD 设置安全的管理员初始密码");
    }
}
