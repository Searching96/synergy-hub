package com.synergyhub.service.security;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logLoginSuccess(User user, String ipAddress, String userAgent) {
        createAuditLog(user, AuditEventType.LOGIN_SUCCESS,
                "User logged in successfully", ipAddress, userAgent);
    }

    @Transactional
    public void logLoginFailed(String email, String ipAddress, String userAgent, String reason) {
        createAuditLog(null, AuditEventType.LOGIN_FAILED,
                String.format("Login failed for email: %s. Reason: %s", email, reason),
                ipAddress, userAgent);
    }

    @Transactional
    public void logTwoFactorRequired(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.TWO_FACTOR_VERIFIED,
                "Two-factor authentication required", ipAddress, null);
    }

    @Transactional
    public void logTwoFactorSuccess(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.TWO_FACTOR_VERIFIED,
                "Two-factor authentication successful", ipAddress, null);
    }

    @Transactional
    public void logTwoFactorFailed(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.TWO_FACTOR_FAILED,
                "Two-factor authentication failed", ipAddress, null);
    }

    @Transactional
    public void logPasswordResetRequested(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.PASSWORD_RESET_REQUESTED,
                "Password reset requested", ipAddress, null);
    }

    @Transactional
    public void logPasswordResetCompleted(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.PASSWORD_RESET_COMPLETED,
                "Password reset completed", ipAddress, null);
    }

    @Transactional
    public void logPasswordChanged(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.PASSWORD_CHANGED,
                "Password changed", ipAddress, null);
    }

    @Transactional
    public void logAccountLocked(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.ACCOUNT_LOCKED,
                "Account locked due to multiple failed login attempts", ipAddress, null);
    }

    @Transactional
    public void logAccountUnlocked(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.ACCOUNT_UNLOCKED,
                "Account unlocked", ipAddress, null);
    }

    @Transactional
    public void logUserCreated(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.USER_CREATED,
                "User account created", ipAddress, null);
    }

    @Transactional
    public void logEmailVerified(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.EMAIL_VERIFIED,
                "Email verified", ipAddress, null);
    }

    @Transactional
    public void logLogout(User user, String ipAddress) {
        createAuditLog(user, AuditEventType.LOGOUT,
                "User logged out", ipAddress, null);
    }

    @Transactional
    public void logSessionRevoked(User user, String ipAddress, String reason) {
        createAuditLog(user, AuditEventType.SESSION_REVOKED,
                "Session revoked: " + reason, ipAddress, null);
    }

    private void createAuditLog(User user, AuditEventType eventType, String details,
                                String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .eventType(eventType.name())
                .eventDetails(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} - {}", eventType, details);
    }
}