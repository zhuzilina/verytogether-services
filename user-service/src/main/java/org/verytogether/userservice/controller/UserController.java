package org.verytogether.userservice.controller;

import org.verytogether.userservice.model.User;
import org.verytogether.userservice.model.UserRole;
import org.verytogether.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
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
public class UserController {

    private final UserService userService;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只有超级管理员可以查看所有用户"));
        }

        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (!currentUser.getId().equals(id) && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只能查看自己的信息"));
        }

        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只有超级管理员可以创建用户"));
        }

        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (!currentUser.getId().equals(id) && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只能更新自己的信息"));
        }

        // 检查是否为超级管理员用户
        if (isSuperAdminUser(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "超级管理员信息只能在配置文件中修改"));
        }

        return userService.updateUser(id, userDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只有超级管理员可以删除用户"));
        }

        // 检查是否为超级管理员用户
        if (isSuperAdminUser(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "超级管理员用户不能被删除"));
        }

        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> roleData, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只有超级管理员可以修改用户角色"));
        }

        // 检查是否为超级管理员用户
        if (isSuperAdminUser(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "超级管理员角色只能在配置文件中修改"));
        }

        String newRole = roleData.get("role");
        if (newRole == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "角色不能为空"));
        }

        try {
            UserRole userRole = UserRole.valueOf(newRole);
            return userService.updateUserRole(id, userRole)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的角色"));
        }
    }

    /**
     * 检查指定用户是否为配置文件中定义的超级管理员
     */
    private boolean isSuperAdminUser(Long userId) {
        // 从配置文件中获取超级管理员用户名
        String adminUsername = getAdminUsername();

        // 查询目标用户
        var targetUser = userService.getUserById(userId);
        return targetUser.map(user ->
            user.getRole() == UserRole.SUPER_ADMIN &&
            user.getUsername().equals(adminUsername)
        ).orElse(false);
    }

    /**
     * 从配置文件中获取超级管理员用户名
     */
    private String getAdminUsername() {
        return adminUsername;
    }

    /**
     * 获取用户角色统计信息
     * 只允许超级管理员访问
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getUserRoleStatistics(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");

        if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "只有超级管理员可以查看用户统计信息"));
        }

        List<User> allUsers = userService.getAllUsers();

        // 统计各角色的用户数量
        long superAdminCount = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.SUPER_ADMIN)
                .count();

        long transactionAdminCount = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.TRANSACTION_ADMIN)
                .count();

        long teacherCount = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.TEACHER)
                .count();

        long studentCount = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .count();

        // 统计活跃用户数量
        long activeUserCount = allUsers.stream()
                .filter(User::getIsActive)
                .count();

        // 统计非活跃用户数量
        long inactiveUserCount = allUsers.size() - activeUserCount;

        // 构建统计结果
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalUsers", allUsers.size());
        statistics.put("activeUsers", activeUserCount);
        statistics.put("inactiveUsers", inactiveUserCount);

        Map<String, Long> roleDistribution = new HashMap<>();
        roleDistribution.put("SUPER_ADMIN", superAdminCount);
        roleDistribution.put("TRANSACTION_ADMIN", transactionAdminCount);
        roleDistribution.put("TEACHER", teacherCount);
        roleDistribution.put("STUDENT", studentCount);
        statistics.put("roleDistribution", roleDistribution);

        // 添加角色百分比
        Map<String, Double> rolePercentages = new HashMap<>();
        if (allUsers.size() > 0) {
            rolePercentages.put("SUPER_ADMIN", (double) superAdminCount / allUsers.size() * 100);
            rolePercentages.put("TRANSACTION_ADMIN", (double) transactionAdminCount / allUsers.size() * 100);
            rolePercentages.put("TEACHER", (double) teacherCount / allUsers.size() * 100);
            rolePercentages.put("STUDENT", (double) studentCount / allUsers.size() * 100);
        } else {
            rolePercentages.put("SUPER_ADMIN", 0.0);
            rolePercentages.put("TRANSACTION_ADMIN", 0.0);
            rolePercentages.put("TEACHER", 0.0);
            rolePercentages.put("STUDENT", 0.0);
        }
        statistics.put("rolePercentages", rolePercentages);

        return ResponseEntity.ok(statistics);
    }
}