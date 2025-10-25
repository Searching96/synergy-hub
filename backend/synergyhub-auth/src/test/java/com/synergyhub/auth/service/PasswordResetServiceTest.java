package com.synergyhub.auth.service;

import com.synergyhub.auth.dto.PasswordResetConfirm;
import com.synergyhub.auth.dto.PasswordResetRequest;
import com.synergyhub.auth.entity.PasswordResetToken;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.PasswordResetTokenRepository;
import com.synergyhub.auth.repository.UserRepository;
import com.synergyhub.auth.repository.UserSessionRepository;
import com.synergyhub.common.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .userId(1)
            .name("Test User")
            .email("test@example.com")
            .passwordHash("oldHash")
            .organizationId(1)
            .build();
    }

    @Test
    void requestPasswordReset_WithValidEmail_ShouldSendEmail() {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest("test@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        passwordResetService.requestPasswordReset(request);

        // Assert
        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
        verify(auditLogService).log(eq(testUser), eq("PASSWORD_RESET_REQUESTED"), anyString(), isNull(), isNull());
    }

    @Test
    void requestPasswordReset_WithNonExistentEmail_ShouldNotThrowError() {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest("nonexistent@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(request));
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    }

    @Test
    void confirmPasswordReset_WithValidToken_ShouldResetPassword() {
        // Arrange
        String token = "valid-token";
        PasswordResetConfirm request = new PasswordResetConfirm(token, "NewPassword123");
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenId(1)
            .user(testUser)
            .token(token)
            .expiryTime(LocalDateTime.now().plusMinutes(15))
            .used(false)
            .build();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(anyString())).thenReturn("newHash");

        // Act
        passwordResetService.confirmPasswordReset(request);

        // Assert
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(resetToken);
        verify(sessionRepository).revokeAllUserSessions(testUser);
        assertTrue(resetToken.getUsed());
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertFalse(testUser.getAccountLocked());
    }

    @Test
    void confirmPasswordReset_WithExpiredToken_ShouldThrowException() {
        // Arrange
        String token = "expired-token";
        PasswordResetConfirm request = new PasswordResetConfirm(token, "NewPassword123");
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenId(1)
            .user(testUser)
            .token(token)
            .expiryTime(LocalDateTime.now().minusMinutes(1))
            .used(false)
            .build();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> 
            passwordResetService.confirmPasswordReset(request));
        
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmPasswordReset_WithUsedToken_ShouldThrowException() {
        // Arrange
        String token = "used-token";
        PasswordResetConfirm request = new PasswordResetConfirm(token, "NewPassword123");
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenId(1)
            .user(testUser)
            .token(token)
            .expiryTime(LocalDateTime.now().plusMinutes(15))
            .used(true)
            .build();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> 
            passwordResetService.confirmPasswordReset(request));
    }
}