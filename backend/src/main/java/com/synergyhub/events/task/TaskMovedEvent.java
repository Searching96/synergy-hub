package com.synergyhub.events.task;

import com.synergyhub.domain.entity.User;

import lombok.Getter;

@Getter
public class TaskMovedEvent extends TaskEvent {
    private final String fromStatus;
    private final String toStatus;

    public TaskMovedEvent(User actor, String ipAddress, Long projectId, String projectName, String taskTitle, String fromStatus, String toStatus) {
        super(actor, ipAddress, projectId, projectName, taskTitle);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
}