package com.synergyhub.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionTest {

    @Test
    void isValid_WhenNotRevokedAndNotExpired_ShouldReturnTrue() {
        // Arrange
        UserSession session = UserSession.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // Act & Assert
        assertTrue(session.isValid());
    }

    @Test
    void isValid_WhenRevoked_ShouldReturnFalse() {
        // Arrange
        UserSession session = UserSession.builder()
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // Act & Assert
        assertFalse(session.isValid());
    }

    @Test
    void isValid_WhenExpired_ShouldReturnFalse() {
        // Arrange
        UserSession session = UserSession.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // Act & Assert
        assertFalse(session.isValid());
    }

    @Test
    void prePersist_ShouldSetCreatedAt() {
        // Arrange
        UserSession session = UserSession.builder()
                .tokenId("token-id")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // Act
        session.onCreate();

        // Assert
        assertNotNull(session.getCreatedAt());
    }
}