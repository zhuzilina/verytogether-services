package com.example.profileservice.config;

import com.example.profileservice.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                // 允许所有对健康检查端点的访问
                .requestMatchers("/actuator/health/**").permitAll()
                // 允许所有对公开API的访问（将在Controller中处理认证）
                .requestMatchers("/api/profiles/public/**").permitAll()
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