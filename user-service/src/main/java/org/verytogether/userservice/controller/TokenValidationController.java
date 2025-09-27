package org.verytogether.userservice.controller;

import org.verytogether.userservice.model.User;
import org.verytogether.userservice.security.JwtTokenProvider;
import org.verytogether.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:8080",
    "http://localhost:4200",
    "http://localhost:5173",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:8080",
    "http://127.0.0.1:4200",
    "http://127.0.0.1:5173"
})
public class TokenValidationController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    /**
     * 验证JWT token与用户ID是否匹配
     * 用于其他服务验证用户身份
     */
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("token");
            Integer userIdInt = (Integer) request.get("userId");
            Long userId = userIdInt != null ? userIdInt.longValue() : null;

            if (token == null || userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "token and userId are required"
                ));
            }

            // 验证token格式是否有效
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", "Invalid token"
                ));
            }

            // 从token中提取用户名
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // 根据用户名查找用户
            User user = userService.getUserByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", "User not found"
                ));
            }

            // 验证用户ID是否匹配
            boolean isValid = user.getId().equals(userId);

            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "username", username,
                "userId", user.getId(),
                "role", user.getRole().name()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Validation failed: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取当前token对应的用户信息
     * 基于现有的认证机制
     */
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "User not authenticated"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "id", currentUser.getId(),
            "username", currentUser.getUsername(),
            "email", currentUser.getEmail(),
            "fullName", currentUser.getFullName(),
            "role", currentUser.getRole().name(),
            "isActive", currentUser.getIsActive()
        ));
    }

    /**
     * 验证token是否有效（不验证用户ID）
     */
    @PostMapping("/validate-token-only")
    public ResponseEntity<?> validateTokenOnly(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");

            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "token is required"
                ));
            }

            boolean isValid = jwtTokenProvider.validateToken(token);

            if (!isValid) {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", "Invalid token"
                ));
            }

            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userService.getUserByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", "User not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "valid", true,
                "username", username,
                "userId", user.getId(),
                "role", user.getRole().name()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Validation failed: " + e.getMessage()
            ));
        }
    }
}