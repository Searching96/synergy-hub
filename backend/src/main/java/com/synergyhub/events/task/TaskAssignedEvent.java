package com.synergyhub.events.task;

import com.synergyhub.domain.entity.User;

import lombok.Getter;

@Getter
public class TaskAssignedEvent extends TaskEvent {
    private final Long assigneeId;
    private final String assigneeName;

    public TaskAssignedEvent(User actor, String ipAddress, Long projectId, String projectName, String taskTitle, Long assigneeId, String assigneeName) {
        super(actor, ipAddress, projectId, projectName, taskTitle);
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }
}