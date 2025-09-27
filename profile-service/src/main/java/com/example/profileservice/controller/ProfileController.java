package com.example.profileservice.controller;

import com.example.profileservice.entity.Profile;
import com.example.profileservice.service.ProfileService;
import com.example.profileservice.service.UserValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
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
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserValidationService userValidationService;

    @Value("${internal.service.secret:internal-service-secret}")
    private String internalServiceSecret;

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 验证是否为内部服务调用
     */
    private boolean isInternalServiceCall(String token) {
        return token != null && token.contains("internal-admin-service");
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody ProfileCreateRequest request, HttpServletRequest httpRequest) {
        String token = extractTokenFromRequest(httpRequest);

        if (token == null || !userValidationService.validateUserAccess(token, request.getUserId())) {
            return ResponseEntity.status(401).body(Map.of("error", "未授权访问"));
        }

        try {
            Profile profile = profileService.createProfile(request.getUserId(), request.getFirstName(), request.getLastName());
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId, HttpServletRequest httpRequest) {
        String token = extractTokenFromRequest(httpRequest);

        if (token == null || !userValidationService.validateUserAccess(token, userId)) {
            return ResponseEntity.status(401).body(Map.of("error", "未授权访问"));
        }

        return profileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody Profile updatedProfile, HttpServletRequest httpRequest) {
        String token = extractTokenFromRequest(httpRequest);

        if (token == null || !userValidationService.validateUserAccess(token, userId)) {
            return ResponseEntity.status(401).body(Map.of("error", "未授权访问"));
        }

        try {
            Profile profile = profileService.updateProfile(userId, updatedProfile);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long userId, HttpServletRequest httpRequest) {
        String token = extractTokenFromRequest(httpRequest);

        // 如果是内部服务调用，允许删除
        if (token == null || (!isInternalServiceCall(token) && !userValidationService.validateUserAccess(token, userId))) {
            return ResponseEntity.status(401).body(Map.of("error", "未授权访问"));
        }

        try {
            profileService.deleteProfile(userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

  
    // DTO for profile creation
    public static class ProfileCreateRequest {
        private Long userId;
        private String firstName;
        private String lastName;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}