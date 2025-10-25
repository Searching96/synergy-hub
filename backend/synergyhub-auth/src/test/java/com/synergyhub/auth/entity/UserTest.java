package com.synergyhub.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void builder_ShouldCreateUserWithDefaults() {
        // Act
        User user = User.builder()
                .userId(1)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hash")
                .organizationId(1)
                .build();

        // Assert
        assertNotNull(user);
        assertFalse(user.getTwoFactorEnabled());
        assertFalse(user.getAccountLocked());
        assertFalse(user.getEmailVerified());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    void isAccountNonLocked_WhenNotLocked_ShouldReturnTrue() {
        // Arrange
        User user = User.builder()
                .accountLocked(false)
                .build();

        // Act & Assert
        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void isAccountNonLocked_WhenLockedButExpired_ShouldUnlockAndReturnTrue() {
        // Arrange
        User user = User.builder()
                .accountLocked(true)
                .lockUntil(LocalDateTime.now().minusMinutes(1))
                .build();

        // Act
        boolean result = user.isAccountNonLocked();

        // Assert
        assertTrue(result);
        assertFalse(user.getAccountLocked());
        assertNull(user.getLockUntil());
    }

    @Test
    void isAccountNonLocked_WhenLockedAndNotExpired_ShouldReturnFalse() {
        // Arrange
        User user = User.builder()
                .accountLocked(true)
                .lockUntil(LocalDateTime.now().plusMinutes(30))
                .build();

        // Act & Assert
        assertFalse(user.isAccountNonLocked());
        assertTrue(user.getAccountLocked());
    }

    @Test
    void prePersist_ShouldSetCreatedAt() {
        // Arrange
        User user = User.builder()
                .name("Test")
                .email("test@example.com")
                .passwordHash("hash")
                .organizationId(1)
                .build();

        // Act
        user.onCreate();

        // Assert
        assertNotNull(user.getCreatedAt());
        assertTrue(user.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void roles_ShouldBeModifiable() {
        // Arrange
        Role role1 = Role.builder().roleId(1).name("Role1").build();
        Role role2 = Role.builder().roleId(2).name("Role2").build();
        User user = User.builder().roles(new HashSet<>()).build();

        // Act
        user.getRoles().add(role1);
        user.getRoles().add(role2);

        // Assert
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(role1));
        assertTrue(user.getRoles().contains(role2));
    }
}