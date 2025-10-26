package com.synergyhub.service.security;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.entity.Project;
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
    public void createAuditLog(User user, String eventType, String details, String ipAddress) {
        createAuditLog(user, eventType, details, ipAddress, null);
    }

    public void createAuditLog(User user, AuditEventType eventType, String details,
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

    // Overload to accept String eventType
    @Transactional
    public void createAuditLog(User user, String eventType, String details,
                               String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .eventType(eventType)
                .eventDetails(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} - {}", eventType, details);
    }

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

    @Transactional
    public void logProjectCreated(Project project, User actor, String ipAddress) {
        createAuditLog(actor, AuditEventType.PROJECT_CREATED,
                String.format("Project '%s' (ID: %d) created in organization %s",
                        project.getName(), project.getId(), project.getOrganization().getName()),
                ipAddress, null);
    }

    @Transactional
    public void logProjectUpdated(Project project, User actor, String ipAddress) {
        createAuditLog(actor, AuditEventType.PROJECT_UPDATED,
                String.format("Project '%s' (ID: %d) updated",
                        project.getName(), project.getId()),
                ipAddress, null);
    }

    @Transactional
    public void logProjectDeleted(Project project, User actor, String ipAddress) {
        createAuditLog(actor, AuditEventType.PROJECT_DELETED,
                String.format("Project '%s' (ID: %d) archived/deleted",
                        project.getName(), project.getId()),
                ipAddress, null);
    }

    @Transactional
    public void logProjectMemberAdded(Project project, Integer userId, User actor, String ipAddress) {
        createAuditLog(actor, AuditEventType.PROJECT_MEMBER_ADDED,
                String.format("User %d added to project '%s' (ID: %d)",
                        userId, project.getName(), project.getId()),
                ipAddress, null);
    }

    @Transactional
    public void logProjectMemberRemoved(Project project, Integer userId, User actor, String ipAddress) {
        createAuditLog(actor, AuditEventType.PROJECT_MEMBER_REMOVED,
                String.format("User %d removed from project '%s' (ID: %d)",
                        userId, project.getName(), project.getId()),
                ipAddress, null);
    }

    @Transactional
    public void logProjectMemberRoleUpdated(Project project, Integer userId, String newRole, User actor, String ipAddress) {
        createAuditLog(actor, AuditEventType.PROJECT_UPDATED,
                String.format("User %d role updated to '%s' in project '%s' (ID: %d)",
                        userId, newRole, project.getName(), project.getId()),
                ipAddress, null);
    }
}