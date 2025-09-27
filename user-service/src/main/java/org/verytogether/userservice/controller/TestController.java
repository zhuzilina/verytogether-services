package org.verytogether.userservice.controller;

import org.verytogether.userservice.service.UserService;
import org.verytogether.userservice.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 测试控制器 - 用于调试密码验证问题
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final UserService userService;
    private final PasswordEncoderUtil passwordEncoderUtil;

    @Autowired
    public TestController(UserService userService, PasswordEncoderUtil passwordEncoderUtil) {
        this.userService = userService;
        this.passwordEncoderUtil = passwordEncoderUtil;
    }

    @GetMapping("/password-test")
    public ResponseEntity<?> testPasswordEncoding() {
        try {
            // 测试密码编码
            String rawPassword = "admin123";
            String encodedPassword = passwordEncoderUtil.encode(rawPassword);

            // 测试密码验证
            boolean matches = passwordEncoderUtil.matches(rawPassword, encodedPassword);

            // 获取数据库中的密码
            String dbPassword = userService.getUserByUsername("admin")
                    .map(user -> user.getPassword())
                    .orElse("用户不存在");

            // 测试数据库密码验证
            boolean dbMatches = passwordEncoderUtil.matches(rawPassword, dbPassword);

            return ResponseEntity.ok(Map.of(
                "rawPassword", rawPassword,
                "newEncodedPassword", encodedPassword,
                "dbPassword", dbPassword,
                "newPasswordMatches", matches,
                "dbPasswordMatches", dbMatches,
                "passwordEncoderClass", passwordEncoderUtil.getClass().getName()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "stackTrace", e.getStackTrace()[0].toString()
            ));
        }
    }

    @PostMapping("/validate-login")
    public ResponseEntity<?> validateLogin(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            var userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            var user = userOpt.get();
            boolean passwordValid = userService.validatePassword(username, password);

            return ResponseEntity.ok(Map.of(
                "username", username,
                "passwordValid", passwordValid,
                "userExists", true,
                "userRole", user.getRole().name(),
                "dbPassword", user.getPassword()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/generate-hash")
    public ResponseEntity<?> generateHash(@RequestBody Map<String, String> data) {
        try {
            String password = data.get("password");
            String encodedPassword = passwordEncoderUtil.encode(password);

            return ResponseEntity.ok(Map.of(
                "password", password,
                "encodedPassword", encodedPassword,
                "encoderClass", passwordEncoderUtil.getClass().getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}