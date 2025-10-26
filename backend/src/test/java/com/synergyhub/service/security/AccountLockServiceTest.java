package com.synergyhub.service.security;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLockServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;  // ✅ Added

    @Mock
    private EmailService emailService;  // ✅ Added

    @InjectMocks
    private AccountLockService accountLockService;

    private User testUser;
    private static final String TEST_IP = "192.168.1.1";  // ✅ Added

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountLockService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(accountLockService, "lockDurationMinutes", 30);

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
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void isAccountLocked_WhenAccountNotLocked_ShouldReturnFalse() {
        // Given
        testUser.setAccountLocked(false);

        // When
        boolean result = accountLockService.isAccountLocked(testUser);

        // Then
        assertThat(result).isFalse();
        verify(auditLogService, never()).logAccountUnlocked(any(), anyString());  // ✅ Added
    }

    @Test
    void isAccountLocked_WhenAccountLockedButExpired_ShouldUnlockAndReturnFalse() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().minusMinutes(1)); // Expired lock

        // When
        boolean result = accountLockService.isAccountLocked(testUser);

        // Then
        assertThat(result).isFalse();
        assertThat(testUser.getAccountLocked()).isFalse();
        assertThat(testUser.getLockUntil()).isNull();
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(testUser);

        // ✅ Verify audit log was called
        verify(auditLogService).logAccountUnlocked(eq(testUser), isNull());
        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("ACCOUNT_UNLOCK_REASON"),
                contains("Lock duration expired"),
                isNull()
        );
    }

    @Test
    void isAccountLocked_WhenAccountLockedAndNotExpired_ShouldReturnTrue() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(10));

        // When
        boolean result = accountLockService.isAccountLocked(testUser);

        // Then
        assertThat(result).isTrue();
        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).logAccountUnlocked(any(), anyString());  // ✅ Added
    }

    @Test
    void handleFailedLogin_WhenBelowMaxAttempts_ShouldIncrementCounter() {
        // Given
        testUser.setFailedLoginAttempts(2);

        // When
        accountLockService.handleFailedLogin(testUser, TEST_IP);  // ✅ Added IP

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(testUser.getAccountLocked()).isFalse();
        verify(userRepository).save(testUser);

        // ✅ Verify audit log for failed attempt
        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("LOGIN_FAILED_ATTEMPT"),
                contains("Failed login attempt #3"),
                eq(TEST_IP)
        );
        verify(auditLogService, never()).logAccountLocked(any(), anyString());
    }
    @Test
    void handleFailedLogin_WhenReachesMaxAttempts_ShouldLockAccount() {
        // Given
        testUser.setFailedLoginAttempts(4); // One more will reach max (5)

        // When
        accountLockService.handleFailedLogin(testUser, TEST_IP);  // ✅ Added IP

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getAccountLocked()).isTrue();
        assertThat(testUser.getLockUntil()).isNotNull();
        assertThat(testUser.getLockUntil()).isAfter(LocalDateTime.now());
        verify(userRepository).save(testUser);

        // ✅ Verify audit log for account lock
        verify(auditLogService).logAccountLocked(testUser, TEST_IP);

        // ✅ Verify email notification was attempted (optional feature)
        verify(emailService).sendAccountLockedEmail(
                eq(testUser.getEmail()),
                eq(testUser),
                eq(TEST_IP)
        );
    }

    @Test
    void resetFailedAttempts_ShouldResetCounterAndUnlock() {
        // Given
        testUser.setFailedLoginAttempts(3);
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(10));

        // When
        accountLockService.resetFailedAttempts(testUser, TEST_IP);  // ✅ Added IP

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        assertThat(testUser.getAccountLocked()).isFalse();
        assertThat(testUser.getLockUntil()).isNull();
        verify(userRepository).save(testUser);

        // ✅ Verify audit log for reset
        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("FAILED_LOGIN_ATTEMPTS_RESET"),
                contains("reset from 3 to 0"),
                eq(TEST_IP)
        );
    }

    @Test
    void resetFailedAttempts_WhenNoFailedAttempts_ShouldNotLogAudit() {
        // Given
        testUser.setFailedLoginAttempts(0);
        testUser.setAccountLocked(false);

        // When
        accountLockService.resetFailedAttempts(testUser, TEST_IP);

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(testUser);

        // ✅ Should not create audit log when there were no previous attempts
        verify(auditLogService, never()).createAuditLog(any(), anyString(), anyString(), anyString());
    }

    @Test
    void unlockAccount_ShouldUnlockAndResetAttempts() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(10));
        testUser.setFailedLoginAttempts(5);

        // When
        accountLockService.unlockAccount(testUser, TEST_IP, "Manual unlock for testing");  // ✅ Added IP and reason

        // Then
        assertThat(testUser.getAccountLocked()).isFalse();
        assertThat(testUser.getLockUntil()).isNull();
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(testUser);

        // ✅ Verify audit logs
        verify(auditLogService).logAccountUnlocked(testUser, TEST_IP);
        verify(auditLogService).createAuditLog(
                eq(testUser),
                eq("ACCOUNT_UNLOCK_REASON"),
                contains("Manual unlock for testing"),
                eq(TEST_IP)
        );
    }

    @Test
    void unlockAccount_WhenNotLocked_ShouldNotLogAudit() {
        // Given
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);

        // When
        accountLockService.unlockAccount(testUser, TEST_IP, "Test");

        // Then
        assertThat(testUser.getAccountLocked()).isFalse();
        verify(userRepository).save(testUser);

        // ✅ Should not create audit log when account wasn't locked
        verify(auditLogService, never()).logAccountUnlocked(any(), anyString());
    }

    // ✅ New tests for utility methods
    @Test
    void getRemainingLockTimeMinutes_WhenNotLocked_ShouldReturnZero() {
        // Given
        testUser.setAccountLocked(false);

        // When
        long remainingTime = accountLockService.getRemainingLockTimeMinutes(testUser);

        // Then
        assertThat(remainingTime).isZero();
    }

    @Test
    void getRemainingLockTimeMinutes_WhenLocked_ShouldReturnRemainingTime() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(15));

        // When
        long remainingTime = accountLockService.getRemainingLockTimeMinutes(testUser);

        // Then
        assertThat(remainingTime).isGreaterThan(0).isLessThanOrEqualTo(15);
    }

    @Test
    void getRemainingLockTimeMinutes_WhenLockExpired_ShouldReturnZero() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().minusMinutes(5));

        // When
        long remainingTime = accountLockService.getRemainingLockTimeMinutes(testUser);

        // Then
        assertThat(remainingTime).isZero();
    }

    @Test
    void isApproachingLockThreshold_WhenBelowThreshold_ShouldReturnFalse() {
        // Given
        testUser.setFailedLoginAttempts(2);

        // When
        boolean result = accountLockService.isApproachingLockThreshold(testUser);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isApproachingLockThreshold_WhenAtThreshold_ShouldReturnTrue() {
        // Given
        testUser.setFailedLoginAttempts(4); // maxLoginAttempts - 1

        // When
        boolean result = accountLockService.isApproachingLockThreshold(testUser);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void getRemainingAttempts_ShouldReturnCorrectValue() {
        // Given
        testUser.setFailedLoginAttempts(3);

        // When
        int remaining = accountLockService.getRemainingAttempts(testUser);

        // Then
        assertThat(remaining).isEqualTo(2); // 5 - 3 = 2
    }

    @Test
    void getRemainingAttempts_WhenExceedsMax_ShouldReturnZero() {
        // Given
        testUser.setFailedLoginAttempts(6);

        // When
        int remaining = accountLockService.getRemainingAttempts(testUser);

        // Then
        assertThat(remaining).isZero();
    }

    @Test
    void forceAccountLock_ByAdmin_ShouldLockAndLogAudit() {
        // Given
        Organization org = Organization.builder()
                .id(1)
                .name("Test Org")
                .build();

        User adminUser = User.builder()
                .id(2)
                .email("admin@example.com")
                .name("Admin User")
                .organization(org)
                .build();

        testUser.setAccountLocked(false);

        // When
        accountLockService.forceAccountLock(
                testUser,
                adminUser,
                "Suspicious activity detected",
                TEST_IP
        );

        // Then
        assertThat(testUser.getAccountLocked()).isTrue();
        assertThat(testUser.getLockUntil()).isNotNull();
        verify(userRepository).save(testUser);

        // ✅ Verify audit log for force lock
        verify(auditLogService).createAuditLog(
                eq(adminUser),
                eq("ACCOUNT_FORCE_LOCKED"),
                contains("Suspicious activity detected"),
                eq(TEST_IP)
        );

        // ✅ Verify email notification
        verify(emailService).sendAccountLockedEmail(
                eq(testUser.getEmail()),
                eq(testUser),
                eq(TEST_IP)
        );
    }

    @Test
    void handleFailedLogin_ShouldNotSendEmailWhenBelowThreshold() {
        // Given
        testUser.setFailedLoginAttempts(2);

        // When
        accountLockService.handleFailedLogin(testUser, TEST_IP);

        // Then
        verify(emailService, never()).sendAccountLockedEmail(anyString(), any(), anyString());
    }
}