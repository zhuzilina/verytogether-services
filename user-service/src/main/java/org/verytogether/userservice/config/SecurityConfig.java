package org.verytogether.userservice.config;

import org.verytogether.userservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置类
 * 配置 Spring Security 以允许 API 访问
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF 保护（REST API 不需要）
            .csrf(csrf -> csrf.disable())

            // 配置请求授权
            .authorizeHttpRequests(authz -> authz
                // 允许所有人对认证端点的访问
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                // 允许所有对健康检查端点的访问
                .requestMatchers("/actuator/health/**").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )

            // 禁用 HTTP Basic 认证
            .httpBasic(basic -> basic.disable())

            // 禁用表单登录
            .formLogin(form -> form.disable())

            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}