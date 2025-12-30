package com.synergyhub.listeners.audit;

import com.synergyhub.events.task.*;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TaskAuditListener {

    private final AuditLogService auditLogService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            "TASK_CREATED",
            String.format("Task '%s' created in project '%s'", event.getTaskTitle(), event.getProjectName()),
            event.getIpAddress(),
            event.getProjectId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskUpdated(TaskUpdatedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            "TASK_UPDATED",
            String.format("Task '%s' updated: %s", event.getTaskTitle(), event.getChanges()),
            event.getIpAddress(),
            event.getProjectId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(TaskAssignedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            "TASK_ASSIGNED",
            String.format("Task '%s' assigned to %s", event.getTaskTitle(), event.getAssigneeName()),
            event.getIpAddress(),
            event.getProjectId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskMoved(TaskMovedEvent event) {
        auditLogService.createAuditLog(
            event.getActor(),
            "TASK_MOVED",
            String.format("Task '%s' moved: %s -> %s", event.getTaskTitle(), event.getFromStatus(), event.getToStatus()),
            event.getIpAddress(),
            event.getProjectId()
        );
    }
}