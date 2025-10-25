package com.synergyhub.service.security;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder()
                .id(1)
                .name("Test Org")
                .build();

        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .organization(org)
                .emailVerified(true)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void logLoginSuccess_ShouldCreateAuditLog() {
        // Given
        String ipAddress = "127.0.0.1";
        String userAgent = "Mozilla/5.0";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logLoginSuccess(testUser, ipAddress, userAgent);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isEqualTo(testUser);
        assertThat(log.getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
        assertThat(log.getUserAgent()).isEqualTo(userAgent);
        assertThat(log.getEventDetails()).contains("successful");
    }

    @Test
    void logLoginFailed_ShouldCreateAuditLog() {
        // Given
        String email = "test@example.com";
        String ipAddress = "127.0.0.1";
        String userAgent = "Mozilla/5.0";
        String reason = "Invalid credentials";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logLoginFailed(email, ipAddress, userAgent, reason);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isNull();
        assertThat(log.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
        assertThat(log.getUserAgent()).isEqualTo(userAgent);
        assertThat(log.getEventDetails()).contains(email).contains(reason);
    }

    @Test
    void logTwoFactorRequired_ShouldCreateAuditLog() {
        // Given
        String ipAddress = "127.0.0.1";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logTwoFactorRequired(testUser, ipAddress);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isEqualTo(testUser);
        assertThat(log.getEventType()).isEqualTo(AuditEventType.TWO_FACTOR_VERIFIED.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    void logTwoFactorSuccess_ShouldCreateAuditLog() {
        // Given
        String ipAddress = "127.0.0.1";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logTwoFactorSuccess(testUser, ipAddress);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isEqualTo(testUser);
        assertThat(log.getEventType()).isEqualTo(AuditEventType.TWO_FACTOR_VERIFIED.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    void logTwoFactorFailed_ShouldCreateAuditLog() {
        // Given
        String ipAddress = "127.0.0.1";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logTwoFactorFailed(testUser, ipAddress);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isEqualTo(testUser);
        assertThat(log.getEventType()).isEqualTo(AuditEventType.TWO_FACTOR_FAILED.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    void logPasswordResetRequested_ShouldCreateAuditLog() {
        // Given
        String ipAddress = "127.0.0.1";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logPasswordResetRequested(testUser, ipAddress);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isEqualTo(testUser);
        assertThat(log.getEventType()).isEqualTo(AuditEventType.PASSWORD_RESET_REQUESTED.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    void logPasswordResetCompleted_ShouldCreateAuditLog() {
        // Given
        String ipAddress = "127.0.0.1";
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditLogService.logPasswordResetCompleted(testUser, ipAddress);

        // Then
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUser()).isEqualTo(testUser);
        assertThat(log.getEventType()).isEqualTo(AuditEventType.PASSWORD_RESET_COMPLETED.name());
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }
}