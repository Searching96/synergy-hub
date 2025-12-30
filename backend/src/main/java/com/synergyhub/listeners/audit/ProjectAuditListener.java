package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.project.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProjectAuditListener {

    private final AuditLogService auditLogService;

    // ✅ LIFECYCLE EVENTS

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectCreated(ProjectCreatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.PROJECT_CREATED.name(),
            String.format("Project '%s' (ID: %d) created", event.getProject().getName(), event.getProject().getId()),
            event.getIpAddress(),
            event.getProject().getId() // ✅ Pass Project ID
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectUpdated(ProjectUpdatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.PROJECT_UPDATED.name(),
            String.format("Project '%s' (ID: %d) updated", event.getProject().getName(), event.getProject().getId()),
            event.getIpAddress(),
            event.getProject().getId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectArchived(ProjectArchivedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.PROJECT_DELETED.name(),
            String.format("Project '%s' (ID: %d) archived", event.getProject().getName(), event.getProject().getId()),
            event.getIpAddress(),
            event.getProject().getId()
        );
    }

    // ✅ MEMBERSHIP EVENTS

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberAdded(ProjectMemberAddedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.PROJECT_MEMBER_ADDED.name(),
            String.format("User %s added to project '%s' (Role: %s)", 
                event.getAddedUser().getEmail(), event.getProject().getName(), event.getRole()),
            event.getIpAddress(),
            event.getProject().getId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberRemoved(ProjectMemberRemovedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.PROJECT_MEMBER_REMOVED.name(),
            String.format("User ID %d removed from project '%s'", event.getRemovedUserId(), event.getProject().getName()),
            event.getIpAddress(),
            event.getProject().getId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberRoleUpdated(ProjectMemberRoleUpdatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            AuditEventType.PROJECT_UPDATED.name(), // Using generic update or specific role update type
            String.format("User ID %d role updated to '%s' in project '%s'", 
                event.getUserId(), event.getNewRole(), event.getProject().getName()),
            event.getIpAddress(),
            event.getProject().getId()
        );
    }
}