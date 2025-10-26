package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.ChangePasswordRequest;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.util.PasswordValidator;
import com.synergyhub.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ChangePasswordService changePasswordService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();
        testUser.setPasswordHash("$2a$10$oldHashedPassword");
    }

    @Test
    void changePassword_WithValidCurrentPassword_ShouldUpdatePassword() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewSecurePass123")
                .build();

        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(passwordValidator.isValid(request.getNewPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword()))
                .thenReturn("$2a$10$newHashedPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        changePasswordService.changePassword(testUser, request, "127.0.0.1");

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$newHashedPassword");

        verify(userSessionRepository).revokeAllUserSessions(testUser);
        verify(auditLogService).logPasswordChanged(testUser, "127.0.0.1");
    }

    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldThrowBadRequestException() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123")
                .newPassword("NewSecurePass123")
                .build();

        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPasswordHash()))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> changePasswordService.changePassword(testUser, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
        verify(userSessionRepository, never()).revokeAllUserSessions(any());
    }

    @Test
    void changePassword_WithWeakNewPassword_ShouldThrowBadRequestException() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("weak")
                .build();

        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(passwordValidator.isValid(request.getNewPassword())).thenReturn(false);
        when(passwordValidator.getRequirements())
                .thenReturn("Password must be at least 8 characters");

        // When & Then
        assertThatThrownBy(() -> changePasswordService.changePassword(testUser, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password does not meet requirements");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WithSamePassword_ShouldThrowBadRequestException() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("SamePassword123")
                .newPassword("SamePassword123")
                .build();

        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(passwordValidator.isValid(request.getNewPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), testUser.getPasswordHash()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> changePasswordService.changePassword(testUser, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password must be different");

        verify(userRepository, never()).save(any());
    }
}