package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.PasswordResetToken;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.dto.request.PasswordResetConfirmRequest;
import com.synergyhub.dto.request.PasswordResetRequest;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.exception.InvalidTokenException;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.PasswordResetTokenRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.util.EmailService;
import com.synergyhub.util.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "resetTokenExpiryMinutes", 15);

        testOrganization = Organization.builder()
                .id(1)
                .name("Test Organization")
                .build();

        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("$2a$10$oldHashedPassword")
                .organization(testOrganization)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void requestPasswordReset_WithValidEmail_ShouldGenerateTokenAndSendEmail() {
        // Given
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(testUser));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // When
        passwordResetService.requestPasswordReset(request, "127.0.0.1");

        // Then
        verify(passwordResetTokenRepository).invalidateAllUserTokens(testUser);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.getUsed()).isFalse();
        assertThat(savedToken.getExpiryTime()).isAfter(LocalDateTime.now());
        assertThat(savedToken.getExpiryTime()).isBefore(LocalDateTime.now().plusMinutes(20));

        verify(emailService).sendPasswordResetEmail(eq(request.getEmail()), anyString());
        verify(auditLogService).logPasswordResetRequested(testUser, "127.0.0.1");
    }

    @Test
    void requestPasswordReset_WithNonExistentEmail_ShouldNotRevealUserExistence() {
        // Given
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("nonexistent@example.com")
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        // When
        passwordResetService.requestPasswordReset(request, "127.0.0.1");

        // Then - Should not throw exception (for security reasons)
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(auditLogService, never()).logPasswordResetRequested(any(), anyString());
    }

    @Test
    void requestPasswordReset_ShouldInvalidateOldTokens() {
        // Given
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(testUser));

        // When
        passwordResetService.requestPasswordReset(request, "127.0.0.1");

        // Then
        verify(passwordResetTokenRepository).invalidateAllUserTokens(testUser);
    }

    @Test
    void resetPassword_WithValidToken_ShouldResetPasswordAndRevokeAllSessions() {
        // Given
        String token = "valid-reset-token";
        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("NewSecurePass123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(false)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));
        when(passwordValidator.isValid(request.getNewPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword()))
                .thenReturn("$2a$10$newHashedPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // When
        passwordResetService.resetPassword(request, "127.0.0.1");

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$newHashedPassword");

        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken updatedToken = tokenCaptor.getValue();
        assertThat(updatedToken.getUsed()).isTrue();

        verify(userSessionRepository).revokeAllUserSessions(testUser);
        verify(auditLogService).logPasswordResetCompleted(testUser, "127.0.0.1");
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldThrowInvalidTokenException() {
        // Given
        String token = "invalid-token";
        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("NewSecurePass123")
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request, "127.0.0.1"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
        verify(userSessionRepository, never()).revokeAllUserSessions(any());
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowInvalidTokenException() {
        // Given
        String token = "expired-token";
        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("NewSecurePass123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(false)
                .expiryTime(LocalDateTime.now().minusMinutes(1)) // Expired
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request, "127.0.0.1"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_WithUsedToken_ShouldThrowInvalidTokenException() {
        // Given
        String token = "used-token";
        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("NewSecurePass123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(true) // Already used
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request, "127.0.0.1"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("already been used");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_WithWeakPassword_ShouldThrowBadRequestException() {
        // Given
        String token = "valid-token";
        PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("weak")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(false)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));
        when(passwordValidator.isValid(request.getNewPassword())).thenReturn(false);
        when(passwordValidator.getRequirements())
                .thenReturn("Password must be at least 8 characters");

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password does not meet requirements");

        verify(userRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void validateResetToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = "valid-token";

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(false)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));

        // When
        boolean result = passwordResetService.validateResetToken(token);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateResetToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String token = "invalid-token";

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.empty());

        // When
        boolean result = passwordResetService.validateResetToken(token);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateResetToken_WithExpiredToken_ShouldReturnFalse() {
        // Given
        String token = "expired-token";

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(false)
                .expiryTime(LocalDateTime.now().minusMinutes(1))
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));

        // When
        boolean result = passwordResetService.validateResetToken(token);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateResetToken_WithUsedToken_ShouldReturnFalse() {
        // Given
        String token = "used-token";

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1)
                .user(testUser)
                .token(token)
                .used(true)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));

        // When
        boolean result = passwordResetService.validateResetToken(token);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void cleanupExpiredTokens_ShouldDeleteExpiredTokens() {
        // When
        passwordResetService.cleanupExpiredTokens();

        // Then
        verify(passwordResetTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}