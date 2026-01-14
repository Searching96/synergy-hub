package com.synergyhub.config;

import com.synergyhub.repository.EmailVerificationRepository;
import com.synergyhub.repository.LoginAttemptRepository;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.PasswordResetTokenRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.OrganizationContext;
import com.synergyhub.domain.entity.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksConfig {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserSessionRepository userSessionRepository;
    private final OrganizationRepository organizationRepository; // Added dependency

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        log.info("Running scheduled task: cleanup expired password reset tokens");
        LocalDateTime now = LocalDateTime.now();

        List<Organization> organizations = organizationRepository.findAll();
        for (Organization org : organizations) {
            OrganizationContext.setcurrentOrgId(org.getId());
            try {
                // Assuming the repository respects the OrganizationContext via AOP or Filter
                passwordResetTokenRepository.deleteExpiredTokens(now);
            } catch (Exception e) {
                log.error("Failed to cleanup password reset tokens for organization: {}", org.getId(), e);
            } finally {
                OrganizationContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredEmailVerifications() {
        log.info("Running scheduled task: cleanup expired email verifications");
        LocalDateTime now = LocalDateTime.now();
        
        List<Organization> organizations = organizationRepository.findAll();
        for (Organization org : organizations) {
            OrganizationContext.setcurrentOrgId(org.getId());
            try {
                emailVerificationRepository.deleteExpiredVerifications(now);
            } catch (Exception e) {
                log.error("Failed to cleanup email verifications for organization: {}", org.getId(), e);
            } finally {
                OrganizationContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldLoginAttempts() {
        log.info("Running scheduled task: cleanup old login attempts");
        LocalDateTime before = LocalDateTime.now().minusDays(30);
        loginAttemptRepository.deleteOldAttempts(before);
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Running scheduled task: cleanup expired and revoked sessions");
        LocalDateTime now = LocalDateTime.now();
        // Sessions might be global or tenant scoped, but usually session cleanup is user-centric. 
        // Leaving as is unless specifically asked, as SessionService snippet didn't suggest context.
        userSessionRepository.cleanupExpiredAndRevokedSessions(now);
    }
}