package org.verytogether.userservice.model;

public enum UserRole {
    SUPER_ADMIN("超级管理员"),
    TRANSACTION_ADMIN("事务管理员"),
    TEACHER("教师用户"),
    STUDENT("学生用户");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}