package com.synergyhub.events.task;

import com.synergyhub.domain.entity.User;

import lombok.Getter;

@Getter
public class TaskAssignedEvent extends TaskEvent {
    private final Integer assigneeId;
    private final String assigneeName;

    public TaskAssignedEvent(User actor, String ipAddress, Integer projectId, String projectName, String taskTitle, Integer assigneeId, String assigneeName) {
        super(actor, ipAddress, projectId, projectName, taskTitle);
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }
}