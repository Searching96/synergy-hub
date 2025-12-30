package com.synergyhub.events.task;

import com.synergyhub.domain.entity.User;

import lombok.Getter;

@Getter
public class TaskUpdatedEvent extends TaskEvent {
    private final String changes;

    public TaskUpdatedEvent(User actor, String ipAddress, Integer projectId, String projectName, String taskTitle, String changes) {
        super(actor, ipAddress, projectId, projectName, taskTitle);
        this.changes = changes;
    }
}