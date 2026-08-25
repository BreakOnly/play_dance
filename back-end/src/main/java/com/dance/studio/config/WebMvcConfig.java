package com.dance.studio.config;

import com.dance.studio.common.context.LoginUser;
import com.dance.studio.common.util.JwtUtil;
import com.dance.studio.interceptor.AuthInterceptor;
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

    @Value("${app.storage.local-dir:./uploads}")
    private String localDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后台接口
        registry.addInterceptor(new AuthInterceptor(jwtUtil, LoginUser.TYPE_ADMIN))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        // 小程序接口
        registry.addInterceptor(new AuthInterceptor(jwtUtil, LoginUser.TYPE_STUDENT))
                .addPathPatterns("/api/mp/**")
                .excludePathPatterns("/api/mp/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地存储文件访问（开发期）
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + localDir + "/");
    }
}
