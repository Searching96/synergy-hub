package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.auth.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventListener {
    private final AuditLogService auditLogService;

    @EventListener
    public void onLoginFailed(LoginFailedEvent event) {
        // Actor is null for login failure
        auditLogService.createAuditLog(
            null, 
            AuditEventType.LOGIN_FAILED.name(),
            String.format("Login failed for email: %s. Reason: %s", event.getEmail(), event.getReason()),
            event.getIpAddress(),
            event.getUserAgent(),
            null // Project ID is null
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
    public void onTwoFactorFailed(TwoFactorFailedEvent event) {
        auditLogService.createAuditLog(
            event.getUser(),
            AuditEventType.TWO_FACTOR_FAILED.name(),
            "Two-factor authentication failed",
            event.getIpAddress(),
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
            null
        );
    }
}