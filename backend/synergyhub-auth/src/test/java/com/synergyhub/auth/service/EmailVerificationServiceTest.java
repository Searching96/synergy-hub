package com.synergyhub.auth.service;

import com.synergyhub.auth.entity.EmailVerification;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.EmailVerificationRepository;
import com.synergyhub.auth.repository.UserRepository;
import com.synergyhub.common.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationRepository verificationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .userId(1)
            .name("Test User")
            .email("test@example.com")
            .passwordHash("hashedPassword")
            .organizationId(1)
            .emailVerified(false)
            .build();
    }

    @Test
    void sendVerificationEmail_ShouldCreateTokenAndSendEmail() {
        // Arrange
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        emailVerificationService.sendVerificationEmail(testUser);

        // Assert
        verify(verificationRepository).deleteByUser(testUser);
        verify(verificationRepository).save(any(EmailVerification.class));
        verify(emailService).sendVerificationEmail(eq(testUser), anyString());
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyEmail() {
        // Arrange
        String token = "valid-token";
        EmailVerification verification = EmailVerification.builder()
            .verificationId(1)
            .user(testUser)
            .token(token)
            .verified(false)
            .expiryTime(LocalDateTime.now().plusHours(24))
            .build();

        when(verificationRepository.findByToken(token)).thenReturn(Optional.of(verification));

        // Act
        emailVerificationService.verifyEmail(token);

        // Assert
        assertTrue(testUser.getEmailVerified());
        assertTrue(verification.getVerified());
        verify(userRepository).save(testUser);
        verify(verificationRepository).save(verification);
        verify(auditLogService).log(eq(testUser), eq("EMAIL_VERIFIED"), anyString(), isNull(), isNull());
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowException() {
        // Arrange
        String token = "expired-token";
        EmailVerification verification = EmailVerification.builder()
            .verificationId(1)
            .user(testUser)
            .token(token)
            .verified(false)
            .expiryTime(LocalDateTime.now().minusHours(1))
            .build();

        when(verificationRepository.findByToken(token)).thenReturn(Optional.of(verification));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () ->
            emailVerificationService.verifyEmail(token));
        
        assertFalse(testUser.getEmailVerified());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_WithAlreadyVerifiedToken_ShouldThrowException() {
        // Arrange
        String token = "already-verified";
        EmailVerification verification = EmailVerification.builder()
            .verificationId(1)
            .user(testUser)
            .token(token)
            .verified(true)
            .expiryTime(LocalDateTime.now().plusHours(24))
            .build();

        when(verificationRepository.findByToken(token)).thenReturn(Optional.of(verification));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () ->
            emailVerificationService.verifyEmail(token));
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldThrowException() {
        // Arrange
        when(verificationRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidTokenException.class, () ->
            emailVerificationService.verifyEmail("invalid-token"));
    }

    @Test
    void resendVerificationEmail_WithValidEmail_ShouldSendEmail() {
        // Arrange
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        emailVerificationService.resendVerificationEmail("test@example.com");

        // Assert
        verify(verificationRepository).deleteByUser(testUser);
        verify(verificationRepository).save(any(EmailVerification.class));
        verify(emailService).sendVerificationEmail(eq(testUser), anyString());
    }

    @Test
    void resendVerificationEmail_WithAlreadyVerifiedEmail_ShouldThrowException() {
        // Arrange
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            emailVerificationService.resendVerificationEmail("test@example.com"));
    }

    @Test
    void resendVerificationEmail_WithNonExistentEmail_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidTokenException.class, () ->
            emailVerificationService.resendVerificationEmail("nonexistent@example.com"));
    }
}