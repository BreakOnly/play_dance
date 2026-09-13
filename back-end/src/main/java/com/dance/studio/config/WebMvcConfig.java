package com.dance.studio.config;

import com.dance.studio.common.context.LoginUser;
import com.dance.studio.common.util.JwtUtil;
import com.dance.studio.entity.User;
import com.dance.studio.interceptor.AuthInterceptor;
import com.dance.studio.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final com.dance.studio.mapper.AdminMapper adminMapper;

    @Value("${app.cors-origins:}")
    private String corsOrigins;

    @Value("${app.storage.local-dir:./uploads}")
    private String localDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后台接口
        registry.addInterceptor(
                        new AuthInterceptor(
                                jwtUtil,
                                LoginUser.TYPE_ADMIN,
                                u -> {
                                    var a = adminMapper.findById(u.getUserId());
                                    return a != null
                                            && a.getStatus() == 1
                                            && Long.valueOf(1).equals(a.getStoreId())
                                            && java.util.Objects.equals(
                                                    a.getTokenVersion(), u.getTokenVersion());
                                }))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        // 小程序接口：token 有效还需学员记录存在且未停用（防重置库后旧 token 引发 NPE）
        registry.addInterceptor(new AuthInterceptor(jwtUtil, LoginUser.TYPE_USER, this::userExists))
                .addPathPatterns("/api/mp/**")
                .excludePathPatterns("/api/mp/auth/login");
    }

    private boolean userExists(LoginUser login) {
        User s = userMapper.findById(login.getUserId());
        return s != null
                && Long.valueOf(1).equals(s.getStoreId())
                && Long.valueOf(1).equals(login.getStoreId());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsOrigins.isBlank()) return;
        registry.addMapping("/**")
                .allowedOrigins(corsOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地存储文件访问（开发期）
        registry.addResourceHandler("/files/**").addResourceLocations("file:" + localDir + "/");
    }
}
