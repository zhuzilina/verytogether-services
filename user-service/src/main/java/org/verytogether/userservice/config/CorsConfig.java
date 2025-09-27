package org.verytogether.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类
 * 支持前后端分离架构的跨域请求
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 应用到所有路径
                .allowedOrigins(
                    "http://localhost:3000",     // React 默认端口
                    "http://localhost:8080",     // Vue 默认端口
                    "http://localhost:4200",     // Angular 默认端口
                    "http://localhost:5173",     // Vite 默认端口
                    "http://127.0.0.1:3000",    // 本地 IP
                    "http://127.0.0.1:8080",
                    "http://127.0.0.1:4200",
                    "http://127.0.0.1:5173"
                ) // 允许的前端域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的 HTTP 方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许发送凭证信息（如 cookies）
                .maxAge(3600); // 预检请求的有效期，单位为秒
    }
}