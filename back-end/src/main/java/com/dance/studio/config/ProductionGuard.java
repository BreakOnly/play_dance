package com.dance.studio.config;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.springframework.core.env.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionGuard {
    private final Environment env;

    @PostConstruct
    public void validate() {
        if (env.acceptsProfiles(Profiles.of("local", "test"))) return;
        if (env.getProperty("app.wechat.mock", Boolean.class, false))
            throw new IllegalStateException("非本地环境禁止微信模拟登录");
        for (String key : new String[] {"app.wechat.appid", "app.wechat.secret"})
            if (env.getProperty(key, "").isBlank())
                throw new IllegalStateException("生产缺少配置：" + key);
        if (!"oss".equals(env.getProperty("app.storage.type")))
            throw new IllegalStateException("生产使用私有 OSS bucket，图片按对象公开，视频鉴权签名");
        for (String key :
                new String[] {
                    "app.storage.oss.endpoint",
                    "app.storage.oss.bucket",
                    "app.storage.oss.access-key-id",
                    "app.storage.oss.access-key-secret"
                })
            if (env.getProperty(key, "").isBlank())
                throw new IllegalStateException("生产缺少配置：" + key);
    }
}
