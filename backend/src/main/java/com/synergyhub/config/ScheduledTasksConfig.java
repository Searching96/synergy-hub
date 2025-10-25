package com.synergyhub.config;

import com.synergyhub.repository.EmailVerificationRepository;
import com.synergyhub.repository.LoginAttemptRepository;
import com.synergyhub.repository.PasswordResetTokenRepository;
import com.synergyhub.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)  // ADD THIS LINE
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksConfig {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserSessionRepository userSessionRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredPasswordResetTokens() {
        log.info("Running scheduled task: cleanup expired password reset tokens");
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository.deleteExpiredTokens(now);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredEmailVerifications() {
        log.info("Running scheduled task: cleanup expired email verifications");
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.deleteExpiredVerifications(now);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldLoginAttempts() {
        log.info("Running scheduled task: cleanup old login attempts");
        LocalDateTime before = LocalDateTime.now().minusDays(30);
        loginAttemptRepository.deleteOldAttempts(before);
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupExpiredSessions() {
        log.info("Running scheduled task: cleanup expired and revoked sessions");
        LocalDateTime now = LocalDateTime.now();
        userSessionRepository.cleanupExpiredAndRevokedSessions(now);
    }
}