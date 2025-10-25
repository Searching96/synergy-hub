package com.synergyhub.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void isExpired_WithFutureExpiry_ShouldReturnFalse() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        // Act & Assert
        assertFalse(token.isExpired());
    }

    @Test
    void isExpired_WithPastExpiry_ShouldReturnTrue() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .expiryTime(LocalDateTime.now().minusMinutes(1))
                .build();

        // Act & Assert
        assertTrue(token.isExpired());
    }

    @Test
    void isValid_WhenNotUsedAndNotExpired_ShouldReturnTrue() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .used(false)
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        // Act & Assert
        assertTrue(token.isValid());
    }

    @Test
    void isValid_WhenUsed_ShouldReturnFalse() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .used(true)
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        // Act & Assert
        assertFalse(token.isValid());
    }

    @Test
    void isValid_WhenExpired_ShouldReturnFalse() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .used(false)
                .expiryTime(LocalDateTime.now().minusMinutes(1))
                .build();

        // Act & Assert
        assertFalse(token.isValid());
    }

    @Test
    void prePersist_ShouldSetCreatedAt() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .token("token")
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        // Act
        token.onCreate();

        // Assert
        assertNotNull(token.getCreatedAt());
    }
}