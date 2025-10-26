package com.synergyhub.service.security;

import com.synergyhub.domain.entity.LoginAttempt;
import com.synergyhub.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final AuditLogService auditLogService;  // ✅ Added

    @Transactional
    public void recordLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();

        loginAttemptRepository.save(attempt);
        log.debug("Recorded login attempt for email: {}, success: {}", email, success);

        // Create audit log for all login attempts
        String eventType = success ? "LOGIN_ATTEMPT_SUCCESS" : "LOGIN_ATTEMPT_FAILED";
        String details = String.format("%s login attempt for email: %s",
                success ? "Successful" : "Failed", email);

        auditLogService.createAuditLog(null, eventType, details, ipAddress, null);
    }

    @Transactional(readOnly = true)
    public List<LoginAttempt> getRecentFailedAttempts(String email, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        List<LoginAttempt> attempts = loginAttemptRepository.findRecentAttemptsByEmail(email, since);

        return attempts.stream()
                .filter(attempt -> !attempt.getSuccess())
                .toList();
    }

    @Transactional(readOnly = true)
    public long countRecentFailedAttempts(String email, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAttemptRepository.countFailedAttemptsByEmail(email, since);
    }

    @Transactional
    public void cleanupOldAttempts(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        loginAttemptRepository.deleteOldAttempts(before);

        log.info("Cleaned up login attempts older than {} days", days);

        // ✅ Create audit log for cleanup operation
        auditLogService.createAuditLog(
                null,
                "LOGIN_ATTEMPTS_CLEANUP",
                String.format("Cleaned up login attempts older than %d days", days),
                "system",
                null
        );
    }
}
