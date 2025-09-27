package org.verytogether.userservice.service;

import org.verytogether.userservice.model.User;
import org.verytogether.userservice.model.UserRole;
import org.verytogether.userservice.repository.UserRepository;
import org.verytogether.userservice.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderUtil passwordEncoderUtil;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoderUtil passwordEncoderUtil) {
        this.userRepository = userRepository;
        this.passwordEncoderUtil = passwordEncoderUtil;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // 如果邮箱不为空，检查是否已存在
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
        }

        // 加密密码
        String encodedPassword = passwordEncoderUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);

        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            // 用户名不允许修改，保持原值
            // user.setUsername(userDetails.getUsername());  // 注释掉这行

            // 如果提供了邮箱，则更新邮箱
            if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
                // 检查邮箱是否已被其他用户使用
                if (!user.getEmail().equals(userDetails.getEmail()) && userRepository.existsByEmail(userDetails.getEmail())) {
                    throw new RuntimeException("Email already exists");
                }
                user.setEmail(userDetails.getEmail());
            } else if (userDetails.getEmail() != null && userDetails.getEmail().trim().isEmpty()) {
                // 如果明确提供了空邮箱，则设置为null
                user.setEmail(null);
            }

            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                String encodedPassword = passwordEncoderUtil.encode(userDetails.getPassword());
                user.setPassword(encodedPassword);
            }
            user.setFullName(userDetails.getFullName());
            user.setUpdatedAt(java.time.LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    public boolean deleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> updateUserRole(Long id, UserRole newRole) {
        return userRepository.findById(id).map(user -> {
            user.setRole(newRole);
            user.setUpdatedAt(java.time.LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    public boolean validatePassword(String username, String rawPassword) {
        return getUserByUsername(username)
                .map(user -> passwordEncoderUtil.matches(rawPassword, user.getPassword()))
                .orElse(false);
    }

    public boolean validatePasswordById(Long id, String rawPassword) {
        return getUserById(id)
                .map(user -> passwordEncoderUtil.matches(rawPassword, user.getPassword()))
                .orElse(false);
    }
}