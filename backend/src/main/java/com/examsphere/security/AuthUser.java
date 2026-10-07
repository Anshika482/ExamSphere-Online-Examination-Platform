package com.examsphere.security;

import com.examsphere.entity.Role;

/** The authenticated principal placed in the SecurityContext for each request. */
public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }
}
