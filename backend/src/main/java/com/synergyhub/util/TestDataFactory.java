package com.synergyhub.util;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;

import java.util.HashSet;

public class TestDataFactory {

    public static Organization createOrganization() {
        return createOrganization(1L, "Test Organization");
    }

    public static Organization createOrganization(Long id, String name) {
        return Organization.builder()
                .id(id)
                .name(name)
                .address("123 Test Street")
                .build();
    }

    public static User createUser() {
        return createUser(1L, "test@example.com", "Test User");
    }

    public static User createUser(Long id, String email, String name) {
        Organization org = createOrganization();
        User user = User.builder()
                .id(id)
                .email(email)
                .name(name)
                .passwordHash("$2a$10$encodedPassword")
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
        user.addMembership(org, null);
        return user;
    }

    public static User createUnverifiedUser() {
        User user = createUser();
        user.setEmailVerified(false);
        return user;
    }

    public static User createLockedUser() {
        User user = createUser();
        user.setAccountLocked(true);
        user.setFailedLoginAttempts(5);
        return user;
    }

    public static Role createRole(String name) {
        return createRole(1L, name);
    }

    public static Role createRole(Long id, String name) {
        return Role.builder()
                .id(id)
                .name(name)
                .description(name + " role")
                .permissions(new HashSet<>())
                .build();
    }

    public static User createUserWithRole(String roleName) {
        User user = createUser();
        Role role = createRole(roleName);
        // Add membership with role
        Organization org = user.getMemberships().get(0).getOrganization();
        user.addMembership(org, role);
        return user;
    }
}