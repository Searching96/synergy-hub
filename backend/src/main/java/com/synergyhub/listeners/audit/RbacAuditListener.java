package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.rbac.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RbacAuditListener {
    
    private final AuditLogService auditLogService;

    /**
     * Listen for RoleCreatedEvent and audit it.
     */
    @EventListener
    public void onRoleCreated(RoleCreatedEvent event) {
        log.debug("Auditing role creation: {}", event.getRole().getId());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.ROLE_CREATED.name(),
            String.format("Role created: %s (ID: %d)", 
                         event.getRole().getName(), 
                         event.getRole().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for RoleUpdatedEvent and audit it.
     */
    @EventListener
    public void onRoleUpdated(RoleUpdatedEvent event) {
        log.debug("Auditing role update: {}", event.getRole().getId());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.ROLE_UPDATED.name(),
            String.format("Role updated: %s (ID: %d)", 
                         event.getRole().getName(), 
                         event.getRole().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for RoleDeletedEvent and audit it.
     */
    @EventListener
    public void onRoleDeleted(RoleDeletedEvent event) {
        log.debug("Auditing role deletion: {}", event.getRole().getId());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.ROLE_DELETED.name(),
            String.format("Role deleted: %s (ID: %d)", 
                         event.getRole().getName(), 
                         event.getRole().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for RolePermissionChangedEvent and audit it (CRITICAL for security).
     * This is the most critical audit event.
     */
    @EventListener
    public void onRolePermissionChanged(RolePermissionChangedEvent event) {
        log.debug("Auditing role permission change: role={}, changeType={}", 
                 event.getRole().getId(), event.getChangeType());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.ROLE_PERMISSION_CHANGED.name(),
            String.format("Role permissions %s: %s (ID: %d) - Permissions: %s", 
                         event.getChangeType(),
                         event.getRole().getName(), 
                         event.getRole().getId(),
                         event.getPermissionIds()),
            event.getIpAddress(),
            null,
            null
        );
    }
}
