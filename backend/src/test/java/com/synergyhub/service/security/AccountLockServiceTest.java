package com.synergyhub.service.security;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.repository.UserRepository;
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

    @InjectMocks
    private AccountLockService accountLockService;

    private User testUser;

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
    }

    @Test
    void handleFailedLogin_WhenBelowMaxAttempts_ShouldIncrementCounter() {
        // Given
        testUser.setFailedLoginAttempts(2);

        // When
        accountLockService.handleFailedLogin(testUser);

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(testUser.getAccountLocked()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    void handleFailedLogin_WhenReachesMaxAttempts_ShouldLockAccount() {
        // Given
        testUser.setFailedLoginAttempts(4); // One more will reach max (5)

        // When
        accountLockService.handleFailedLogin(testUser);

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getAccountLocked()).isTrue();
        assertThat(testUser.getLockUntil()).isNotNull();
        assertThat(testUser.getLockUntil()).isAfter(LocalDateTime.now());
        verify(userRepository).save(testUser);
    }

    @Test
    void resetFailedAttempts_ShouldResetCounterAndUnlock() {
        // Given
        testUser.setFailedLoginAttempts(3);
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(10));

        // When
        accountLockService.resetFailedAttempts(testUser);

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        assertThat(testUser.getAccountLocked()).isFalse();
        assertThat(testUser.getLockUntil()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void unlockAccount_ShouldUnlockAndResetAttempts() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().plusMinutes(10));
        testUser.setFailedLoginAttempts(5);

        // When
        accountLockService.unlockAccount(testUser);

        // Then
        assertThat(testUser.getAccountLocked()).isFalse();
        assertThat(testUser.getLockUntil()).isNull();
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(testUser);
    }
}