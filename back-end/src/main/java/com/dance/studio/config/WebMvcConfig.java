package com.dance.studio.config;

import com.dance.studio.common.context.LoginUser;
import com.dance.studio.common.util.JwtUtil;
import com.dance.studio.entity.Student;
import com.dance.studio.interceptor.AuthInterceptor;
import com.dance.studio.mapper.StudentMapper;
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
    private final StudentMapper studentMapper;

    @Value("${app.storage.local-dir:./uploads}")
    private String localDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后台接口
        registry.addInterceptor(new AuthInterceptor(jwtUtil, LoginUser.TYPE_ADMIN))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        // 小程序接口：token 有效还需学员记录存在且未停用（防重置库后旧 token 引发 NPE）
        registry.addInterceptor(new AuthInterceptor(jwtUtil, LoginUser.TYPE_STUDENT, this::studentExists))
                .addPathPatterns("/api/mp/**")
                .excludePathPatterns("/api/mp/auth/login");
    }

    private boolean studentExists(Long studentId) {
        Student s = studentMapper.findById(studentId);
        return s != null && (s.getStatus() == null || s.getStatus() == 1);
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
