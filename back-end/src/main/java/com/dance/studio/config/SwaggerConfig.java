package com.dance.studio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI danceStudioOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("街舞工作室管理系统 API")
                .description("后台 /api/admin/** ; 小程序 /api/mp/**")
                .version("1.0.0"));
    }
}
