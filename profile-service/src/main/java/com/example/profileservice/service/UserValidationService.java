package com.example.profileservice.service;

import com.example.profileservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class UserValidationService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    /**
     * 验证用户是否有权限访问指定的用户档案
     * @param token JWT token
     * @param userId 要访问的用户ID
     * @return 是否有权限
     */
    public boolean validateUserAccess(String token, Long userId) {
        try {
            // 首先验证token是否有效
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }

            // 调用user-service验证token和用户ID是否匹配
            String validationUrl = userServiceUrl + "/api/auth/validate-token";

            Map<String, Object> request = Map.of(
                "token", token,
                "userId", userId
            );

            Map<String, Object> response = restTemplate.postForObject(validationUrl, request, Map.class);

            if (response != null && response.containsKey("valid")) {
                return Boolean.TRUE.equals(response.get("valid"));
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从token中提取用户名
     * @param token JWT token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * 验证token是否有效
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
}