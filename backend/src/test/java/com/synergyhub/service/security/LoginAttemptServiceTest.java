package com.synergyhub.service.security;

import com.synergyhub.domain.entity.LoginAttempt;
import com.synergyhub.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    void recordLoginAttempt_ShouldSaveLoginAttempt() {
        // Given
        String email = "test@example.com";
        String ipAddress = "127.0.0.1";
        boolean success = true;

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);

        // When
        loginAttemptService.recordLoginAttempt(email, ipAddress, success);

        // Then
        verify(loginAttemptRepository).save(captor.capture());
        LoginAttempt savedAttempt = captor.getValue();

        assertThat(savedAttempt.getEmail()).isEqualTo(email);
        assertThat(savedAttempt.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedAttempt.getSuccess()).isEqualTo(success);
    }

    @Test
    void getRecentFailedAttempts_ShouldReturnFailedAttemptsInTimeWindow() {
        // Given
        String email = "test@example.com";
        int minutes = 15;
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);

        List<LoginAttempt> expectedAttempts = Arrays.asList(
                createLoginAttempt(email, "127.0.0.1", false),
                createLoginAttempt(email, "127.0.0.1", false)
        );

        when(loginAttemptRepository.findRecentAttemptsByEmail(eq(email), any(LocalDateTime.class)))
                .thenReturn(expectedAttempts);

        // When
        List<LoginAttempt> result = loginAttemptService.getRecentFailedAttempts(email, minutes);

        // Then
        assertThat(result).hasSize(2);
        verify(loginAttemptRepository).findRecentAttemptsByEmail(eq(email), any(LocalDateTime.class));
    }

    @Test
    void countRecentFailedAttempts_ShouldReturnCount() {
        // Given
        String email = "test@example.com";
        int minutes = 15;

        when(loginAttemptRepository.countFailedAttemptsByEmail(eq(email), any(LocalDateTime.class)))
                .thenReturn(3L);

        // When
        long count = loginAttemptService.countRecentFailedAttempts(email, minutes);

        // Then
        assertThat(count).isEqualTo(3L);
        verify(loginAttemptRepository).countFailedAttemptsByEmail(eq(email), any(LocalDateTime.class));
    }

    @Test
    void cleanupOldAttempts_ShouldDeleteAttemptsOlderThanDays() {
        // Given
        int days = 30;

        // When
        loginAttemptService.cleanupOldAttempts(days);

        // Then
        verify(loginAttemptRepository).deleteOldAttempts(any(LocalDateTime.class));
    }

    private LoginAttempt createLoginAttempt(String email, String ip, boolean success) {
        return LoginAttempt.builder()
                .email(email)
                .ipAddress(ip)
                .success(success)
                .build();
    }
}