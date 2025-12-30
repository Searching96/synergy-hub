package com.synergyhub.events.task;

import com.synergyhub.domain.entity.User;

import lombok.Getter;

public class TaskCreatedEvent extends TaskEvent {
    @Getter private final String projectName; // Extra context often needed for emails

    public TaskCreatedEvent(User actor, String ipAddress, Integer projectId, String projectName, String taskTitle) {
        super(actor, ipAddress, projectId, projectName, taskTitle);
        this.projectName = projectName;
    }
}
