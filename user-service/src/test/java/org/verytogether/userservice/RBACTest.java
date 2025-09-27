package org.verytogether.userservice;

import org.verytogether.userservice.model.User;
import org.verytogether.userservice.model.UserRole;
import org.verytogether.userservice.repository.UserRepository;
import org.verytogether.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RBACTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateUsersWithDifferentRoles() {
        User superAdmin = new User("superadmin", "super@test.com", "password", "超级管理员", UserRole.SUPER_ADMIN);
        User transactionAdmin = new User("transadmin", "trans@test.com", "password", "事务管理员", UserRole.TRANSACTION_ADMIN);
        User studentUser = new User("studentuser", "student@test.com", "password", "学生用户", UserRole.STUDENT);

        User createdSuperAdmin = userService.createUser(superAdmin);
        User createdTransactionAdmin = userService.createUser(transactionAdmin);
        User createdStudentUser = userService.createUser(studentUser);

        assertEquals(UserRole.SUPER_ADMIN, createdSuperAdmin.getRole());
        assertEquals(UserRole.TRANSACTION_ADMIN, createdTransactionAdmin.getRole());
        assertEquals(UserRole.STUDENT, createdStudentUser.getRole());
    }

    @Test
    public void testUpdateUserRole() {
        User user = new User("testuser", "test@test.com", "password", "测试用户", UserRole.STUDENT);
        User createdUser = userService.createUser(user);

        assertEquals(UserRole.STUDENT, createdUser.getRole());

        User updatedUser = userService.updateUserRole(createdUser.getId(), UserRole.TRANSACTION_ADMIN).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(UserRole.TRANSACTION_ADMIN, updatedUser.getRole());
    }

    @Test
    public void testDefaultUserRole() {
        User user = new User("defaultuser", "default@test.com", "password", "默认用户");
        User createdUser = userService.createUser(user);

        assertEquals(UserRole.STUDENT, createdUser.getRole());
    }

    @Test
    public void testGetAllUsers() {
        User user1 = new User("user1", "user1@test.com", "password", "用户1", UserRole.STUDENT);
        User user2 = new User("user2", "user2@test.com", "password", "用户2", UserRole.SUPER_ADMIN);

        userService.createUser(user1);
        userService.createUser(user2);

        List<User> allUsers = userService.getAllUsers();
        assertTrue(allUsers.size() >= 2);
    }
}