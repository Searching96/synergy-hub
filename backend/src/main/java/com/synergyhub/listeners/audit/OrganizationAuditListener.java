package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.organization.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationAuditListener {
    
    private final AuditLogService auditLogService;

    /**
     * Listen for OrganizationCreatedEvent and audit it.
     */
    @EventListener
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        log.debug("Auditing organization creation: {}", event.getOrganization().getId());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.ORGANIZATION_CREATED.name(),
            String.format("Organization created: %s (ID: %d)", 
                         event.getOrganization().getName(), 
                         event.getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }

    /**
     * Listen for OrganizationUpdatedEvent and audit it.
     */
    @EventListener
    public void onOrganizationUpdated(OrganizationUpdatedEvent event) {
        log.debug("Auditing organization update: {}", event.getOrganization().getId());
        
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.ORGANIZATION_UPDATED.name(),
            String.format("Organization updated: %s (ID: %d)", 
                         event.getOrganization().getName(), 
                         event.getOrganization().getId()),
            event.getIpAddress(),
            null,
            null
        );
    }
}
