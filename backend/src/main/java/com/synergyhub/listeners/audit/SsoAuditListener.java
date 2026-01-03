package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.sso.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Audit Listener for SSO Provider Management Events.
 * Captures all SSO configuration changes for compliance auditing.
 *
 * Events Handled:
 * - SsoProviderRegisteredEvent: New provider registration
 * - SsoProviderUpdatedEvent: Configuration updates
 * - SsoProviderSecretRotatedEvent: ⚠️ CRITICAL - Secret rotation
 * - SsoProviderStateChangedEvent: Enable/disable operations
 * - SsoProviderDeletedEvent: Provider deletion
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SsoAuditListener {

    private final AuditLogService auditLogService;

    /**
     * Listen for SsoProviderRegisteredEvent and audit it.
     */
    @EventListener
    public void onSsoProviderRegistered(SsoProviderRegisteredEvent event) {
        log.debug("Auditing SSO provider registration: {}", event.getProvider().getProviderName());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.SSO_PROVIDER_REGISTERED.name(),
            String.format("SSO provider registered: %s (Type: %s, Org ID: %d)", 
                         event.getProvider().getProviderName(),
                         event.getProvider().getProviderType().name(),
                         event.getProvider().getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for SsoProviderUpdatedEvent and audit it.
     */
    @EventListener
    public void onSsoProviderUpdated(SsoProviderUpdatedEvent event) {
        log.debug("Auditing SSO provider update: {}", event.getProvider().getProviderName());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.SSO_PROVIDER_UPDATED.name(),
            String.format("SSO provider updated: %s (ID: %d, Org ID: %d)", 
                         event.getProvider().getProviderName(),
                         event.getProvider().getId(),
                         event.getProvider().getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for SsoProviderSecretRotatedEvent and audit it.
     * ⚠️ CRITICAL: This is a sensitive security operation that must be audited.
     */
    @EventListener
    public void onSsoProviderSecretRotated(SsoProviderSecretRotatedEvent event) {
        log.warn("CRITICAL: Auditing SSO provider secret rotation: {}", event.getProvider().getProviderName());
        
        auditLogService.createAuditLog(
            event.getActor(),
            "SSO_PROVIDER_SECRET_ROTATED",  // Mark as CRITICAL event
            String.format("⚠️ CRITICAL: SSO provider secret rotated: %s (ID: %d, Org ID: %d). This is a security-sensitive operation.", 
                         event.getProvider().getProviderName(),
                         event.getProvider().getId(),
                         event.getProvider().getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for SsoProviderStateChangedEvent and audit it.
     */
    @EventListener
    public void onSsoProviderStateChanged(SsoProviderStateChangedEvent event) {
        log.debug("Auditing SSO provider state change: {}", event.getProvider().getProviderName());
        
        String action = event.getNewState() ? "enabled" : "disabled";
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.SSO_PROVIDER_STATE_CHANGED.name(),
            String.format("SSO provider %s: %s (ID: %d, Org ID: %d)", 
                         action,
                         event.getProvider().getProviderName(),
                         event.getProvider().getId(),
                         event.getProvider().getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for SsoProviderDeletedEvent and audit it.
     */
    @EventListener
    public void onSsoProviderDeleted(SsoProviderDeletedEvent event) {
        log.debug("Auditing SSO provider deletion: {}", event.getProvider().getProviderName());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.SSO_PROVIDER_DELETED.name(),
            String.format("SSO provider deleted: %s (ID: %d, Org ID: %d)", 
                         event.getProvider().getProviderName(),
                         event.getProvider().getId(),
                         event.getProvider().getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }
}
