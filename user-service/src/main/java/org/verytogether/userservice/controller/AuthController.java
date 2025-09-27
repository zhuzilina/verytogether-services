package org.verytogether.userservice.controller;

import org.verytogether.userservice.model.User;
import org.verytogether.userservice.service.UserService;
import org.verytogether.userservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

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
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }

        // 调试日志
        System.out.println("Login attempt for username: " + username);

        var userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("User not found: " + username);
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
        }

        var user = userOpt.get();
        System.out.println("User found: " + user.getUsername());
        System.out.println("User password hash: " + user.getPassword());

        boolean passwordValid = userService.validatePassword(username, password);
        System.out.println("Password validation result: " + passwordValid);

        if (passwordValid) {
            String token = jwtTokenProvider.generateToken(username);
            System.out.println("Login successful, token generated");
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail()); // 允许 null
            userMap.put("fullName", user.getFullName());
            userMap.put("role", user.getRole().name());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userMap);

            return ResponseEntity.ok(response);
        } else {
            System.out.println("Login failed: invalid password");
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        String username = userData.get("username");
        String email = userData.get("email");
        String password = userData.get("password");
        String fullName = userData.get("fullName");
        String roleStr = userData.get("role");

        if (username == null || password == null || fullName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名、密码和姓名是必需的"));
        }

        // 邮箱是可选的，如果提供了空字符串则设置为null
        if (email != null && email.trim().isEmpty()) {
            email = null;
        }

        try {
            User user = new User(username, email, password, fullName);
            User createdUser = userService.createUser(user);

            String token = jwtTokenProvider.generateToken(username);
            return ResponseEntity.status(201).body(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", createdUser.getId(),
                            "username", createdUser.getUsername(),
                            "email", createdUser.getEmail(),
                            "fullName", createdUser.getFullName(),
                            "role", createdUser.getRole().name()
                    )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}