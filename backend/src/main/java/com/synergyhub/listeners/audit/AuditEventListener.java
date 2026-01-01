package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.auth.*;
import com.synergyhub.events.system.LoginAttemptsCleanupEvent;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventListener {
    private final AuditLogService auditLogService;

    // ========== LOGIN EVENTS ==========
    
    @EventListener
    public void onLoginFailed(LoginFailedEvent event) {
        auditLogService.createAuditLog(
            null, 
            AuditEventType.LOGIN_FAILED.name(),
            String.format("Login failed for email: %s. Reason: %s", event.getEmail(), event.getReason()),
            event.getIpAddress(),
            event.getUserAgent(),
            null
        );
    }

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.LOGIN_SUCCESS.name(),
            "User logged in successfully",
            event.getIpAddress(),
            event.getUserAgent(),
            null
        );
    }

    @EventListener
    public void onLogout(LogoutEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.LOGOUT.name(),
            "User logged out",
            event.getIpAddress(),
            null,
            null
        );
    }

    // ========== TWO-FACTOR AUTHENTICATION EVENTS ==========
    
    @EventListener
    public void onTwoFactorFailed(TwoFactorFailedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_FAILED.name(),
            "Two-factor authentication failed",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onTwoFactorSuccess(TwoFactorSuccessEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_VERIFIED.name(),
            "Two-factor authentication successful",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onTwoFactorSetupInitiated(TwoFactorSetupInitiatedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_SETUP_INITIATED.name(),
            "Two-factor authentication setup initiated",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onTwoFactorDisabled(TwoFactorDisabledEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_DISABLED.name(),
            "Two-factor authentication disabled",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onTwoFactorDisableFailed(TwoFactorDisableFailedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_DISABLE_FAILED.name(),
            "Two-factor authentication disable failed - invalid password",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onTwoFactorBackupCodeRegenerated(TwoFactorBackupCodeRegeneratedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_BACKUP_CODE_REGENERATED.name(),
            "Two-factor backup codes regenerated",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onTwoFactorBackupCodeUsed(TwoFactorBackupCodeUsedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_BACKUP_CODE_USED.name(),
            String.format("Two-factor backup code used"),
            event.getIpAddress(),
            null,
            null
        );
    }

    // ========== PASSWORD RESET EVENTS ==========
    
    @EventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.PASSWORD_RESET_REQUESTED.name(),
            String.format("Password reset requested for user: %s", event.getUser().getEmail()),
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onPasswordResetCompleted(PasswordResetCompletedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.PASSWORD_RESET_COMPLETED.name(),
            "Password reset completed successfully",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.PASSWORD_CHANGED.name(),
            "Password changed successfully",
            event.getIpAddress(),
            null,
            null
        );
    }

    // ========== REGISTRATION & EMAIL VERIFICATION EVENTS ==========
    
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.USER_CREATED.name(),
            String.format("User account created: %s", event.getUser().getEmail()),
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onRegistrationFailed(RegistrationFailedEvent event) {
        auditLogService.createAuditLog(
            null,
            "REGISTRATION_FAILED",
            String.format("Registration failed for email: %s. Reason: %s", event.getEmail(), event.getReason()),
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onEmailVerified(EmailVerifiedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.EMAIL_VERIFIED.name(),
            "Email verified successfully",
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onEmailVerificationFailed(EmailVerificationFailedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            "EMAIL_VERIFICATION_FAILED",
            String.format("Email verification failed. Reason: %s", event.getReason()),
            event.getIpAddress(),
            null,
            null
        );
    }

    // ========== ACCOUNT LOCK EVENTS ==========
    
    @EventListener
    public void onAccountLocked(AccountLockedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.ACCOUNT_LOCKED.name(),
            String.format("Account locked due to %d failed login attempts (locked for %d minutes)",
                         event.getFailedAttempts(), event.getLockDurationMinutes()),
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onAccountUnlocked(AccountUnlockedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.ACCOUNT_UNLOCKED.name(),
            String.format("Account unlocked. Reason: %s", event.getReason()),
            event.getIpAddress(),
            null,
            null
        );
    }

    @EventListener
    public void onAccountForceLocked(AccountForceLockedEvent event) {
        auditLogService.createAuditLog(
            event.getAdmin(),
            "ACCOUNT_FORCE_LOCKED",
            String.format("Admin %s (ID: %d) force locked account for user %s (ID: %d). Reason: %s",
                         event.getAdmin().getEmail(), event.getAdmin().getId(),
                         event.getUser().getEmail(), event.getUser().getId(),
                         event.getReason()),
            event.getIpAddress(),
            null,
            null
        );
    }

    // ========== SESSION EVENTS ==========
    
    @EventListener
    public void onAllSessionsRevoked(AllSessionsRevokedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.ALL_SESSIONS_REVOKED.name(),
            "User logged out from all devices",
            event.getIpAddress(),
            null,
            null
        );
    }


    // ========== SYSTEM EVENTS ==========
    
    @EventListener
    public void onLoginAttemptsCleanup(LoginAttemptsCleanupEvent event) {
        auditLogService.createAuditLog(
            null,
            "LOGIN_ATTEMPTS_CLEANUP",
            String.format("Cleaned up %d login attempts older than %d days", 
                         event.getDeletedCount(), event.getDaysRetained()),
            event.getIpAddress(),
            null,
            null
        );
    }
}