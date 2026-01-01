package com.synergyhub.service.security;

import com.synergyhub.domain.entity.LoginAttempt;
import com.synergyhub.events.system.LoginAttemptsCleanupEvent;
import com.synergyhub.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Records a login attempt in the login_attempts table.
     * 
     * NOTE: This does NOT create audit logs - audit logging is handled by
     * LoginSuccessEvent and LoginFailedEvent in the authentication flow.
     * This service only tracks attempts for rate limiting and security analysis.
     */
    @Transactional
    public void recordLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();

        loginAttemptRepository.save(attempt);
        
        log.debug("Recorded login attempt for email: {}, success: {}, IP: {}", 
                 email, success, ipAddress);
    }

    /**
     * Get recent failed login attempts for an email within a time window.
     * Used for rate limiting and security checks.
     */
    @Transactional(readOnly = true)
    public List<LoginAttempt> getRecentFailedAttempts(String email, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        List<LoginAttempt> attempts = loginAttemptRepository.findRecentAttemptsByEmail(email, since);

        return attempts.stream()
                .filter(attempt -> !attempt.getSuccess())
                .toList();
    }

    /**
     * Count recent failed login attempts for an email.
     * Used for rate limiting decisions.
     */
    @Transactional(readOnly = true)
    public long countRecentFailedAttempts(String email, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAttemptRepository.countFailedAttemptsByEmail(email, since);
    }

    /**
     * Get recent failed attempts from a specific IP address.
     * Used for IP-based rate limiting.
     */
    @Transactional(readOnly = true)
    public long countRecentFailedAttemptsByIp(String ipAddress, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAttemptRepository.countFailedAttemptsByIpAddress(ipAddress, since);
    }

    /**
     * Cleanup old login attempts (scheduled job).
     * Publishes event for audit logging.
     */
    @Transactional
    public void cleanupOldAttempts(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        
        // Count before deletion for audit purposes
        long countBeforeDelete = loginAttemptRepository.countByCreatedAtBefore(before);
        
        // Delete old attempts
        loginAttemptRepository.deleteOldAttempts(before);

        log.info("Cleaned up {} login attempts older than {} days", countBeforeDelete, days);

        // ✅ Publish cleanup event for audit logging
        eventPublisher.publishEvent(
            new LoginAttemptsCleanupEvent(days, countBeforeDelete, "system")
        );
    }

    /**
     * Get all login attempts for a user (admin function).
     */
    @Transactional(readOnly = true)
    public List<LoginAttempt> getAllAttemptsByEmail(String email) {
        return loginAttemptRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    /**
     * Get recent login attempts from an IP address (security analysis).
     */
    @Transactional(readOnly = true)
    public List<LoginAttempt> getRecentAttemptsByIp(String ipAddress, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return loginAttemptRepository.findByIpAddressAndCreatedAtAfter(ipAddress, since);
    }
}