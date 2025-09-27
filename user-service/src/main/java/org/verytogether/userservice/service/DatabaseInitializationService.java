package org.verytogether.userservice.service;

import org.verytogether.userservice.model.User;
import org.verytogether.userservice.model.UserRole;
import org.verytogether.userservice.util.PasswordEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
public class DatabaseInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationService.class);

    @Autowired
    private UserService userService;

    // @Autowired
    // private PasswordEncoderUtil passwordEncoderUtil;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Value("${admin.email:admin@system.local}")
    private String adminEmail;

    @Value("${admin.fullName:System Administrator}")
    private String adminFullName;

    @Value("${admin.role:SUPER_ADMIN}")
    private String adminRole;

    @PostConstruct
    @Transactional
    public void initializeAdminUser() {
        try {
            logger.info("开始初始化管理员用户...");

            // 检查管理员用户是否存在
            var existingUser = userService.getUserByUsername(adminUsername);

            if (existingUser.isPresent()) {
                // 用户存在，检查密码是否匹配
                User user = existingUser.get();
                boolean passwordMatches = userService.validatePassword(adminUsername, adminPassword);

                if (passwordMatches) {
                    logger.info("管理员用户 {} 已存在且密码正确", adminUsername);
                } else {
                    // 密码不匹配，更新密码
                    logger.info("管理员用户 {} 存在但密码不匹配，正在更新密码...", adminUsername);

                    // 创建临时用户对象来更新密码
                    User tempUser = new User();
                    tempUser.setUsername(user.getUsername()); // 保持原有用户名
                    tempUser.setPassword(adminPassword);
                    userService.updateUser(user.getId(), tempUser);
                    logger.info("管理员用户 {} 密码已更新", adminUsername);
                }
            } else {
                // 用户不存在，创建新用户
                logger.info("管理员用户 {} 不存在，正在创建...", adminUsername);

                User adminUser = new User(adminUsername, adminEmail, adminPassword, adminFullName, UserRole.valueOf(adminRole));
                adminUser.setIsActive(true);

                userService.createUser(adminUser);
                logger.info("管理员用户 {} 创建成功", adminUsername);
            }

            logger.info("管理员用户初始化完成");

        } catch (Exception e) {
            logger.error("初始化管理员用户时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("初始化管理员用户失败", e);
        }
    }
}