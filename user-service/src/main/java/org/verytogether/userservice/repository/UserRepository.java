package org.verytogether.userservice.repository;

import org.verytogether.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 查询邮箱不为空的用户
    @Query("SELECT u FROM User u WHERE u.email IS NOT NULL AND u.email != ''")
    List<User> findUsersWithEmail();

    // 根据用户名或邮箱查找用户（用于登录等场景）
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR (u.email IS NOT NULL AND u.email = :identifier)")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
}