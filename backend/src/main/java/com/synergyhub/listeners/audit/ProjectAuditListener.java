package com.synergyhub.listeners.audit;

import com.synergyhub.domain.enums.AuditEventType;
import com.synergyhub.events.project.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuditListener {
    
    private final AuditLogService auditLogService;

    // ========== PROJECT LIFECYCLE EVENTS ==========
    
    @EventListener
    public void onProjectCreated(ProjectCreatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(), // ✅ getActor() from BaseEvent (actor)
            AuditEventType.PROJECT_CREATED.name(),
            String.format("Project created: %s (ID: %d)", 
                         event.getProject().getName(), 
                         event.getProject().getId()),
            event.getIpAddress(),
            null,
            event.getProject().getId()
        );
    }

    @EventListener
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(), // ✅ getActor() from BaseEvent (actor)
            AuditEventType.PROJECT_UPDATED.name(),
            String.format("Project updated: %s (ID: %d)", 
                         event.getProject().getName(), 
                         event.getProject().getId()),
            event.getIpAddress(),
            null,
            event.getProject().getId()
        );
    }

    @EventListener
    public void onProjectArchived(ProjectArchivedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(), // ✅ getActor() from BaseEvent (actor)
            AuditEventType.PROJECT_DELETED.name(),
            String.format("Project archived: %s (ID: %d)", 
                         event.getProject().getName(), 
                         event.getProject().getId()),
            event.getIpAddress(),
            null,
            event.getProject().getId()
        );
    }

    // ========== PROJECT MEMBERSHIP EVENTS ==========
    
    @EventListener
    public void onProjectMemberAdded(ProjectMemberAddedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(), // ✅ getActor() from BaseEvent (actor)
            AuditEventType.PROJECT_MEMBER_ADDED.name(),
            String.format("User %s (ID: %d) added to project %s (ID: %d) with role %s",
                         event.getMember().getEmail(),
                         event.getMember().getId(),
                         event.getProject().getName(),
                         event.getProject().getId(),
                         event.getRole()),
            event.getIpAddress(),
            null,
            event.getProject().getId()
        );
    }

    @EventListener
    public void onProjectMemberRemoved(ProjectMemberRemovedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(), // ✅ getActor() from BaseEvent (actor)
            AuditEventType.PROJECT_MEMBER_REMOVED.name(),
            String.format("User (ID: %d) removed from project %s (ID: %d)",
                         event.getRemovedUserId(),
                         event.getProject().getName(),
                         event.getProject().getId()),
            event.getIpAddress(),
            null,
            event.getProject().getId()
        );
    }

    @EventListener
    public void onProjectMemberRoleUpdated(ProjectMemberRoleUpdatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(), // ✅ getActor() from BaseEvent (actor)
            "PROJECT_MEMBER_ROLE_UPDATED",
            String.format("User (ID: %d) role updated to %s in project %s (ID: %d)",
                         event.getUserId(),
                         event.getNewRole(),
                         event.getProject().getName(),
                         event.getProject().getId()),
            event.getIpAddress(),
            null,
            event.getProject().getId()
        );
    }
}