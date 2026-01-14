package com.synergyhub.events.task;

import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;

import lombok.Getter;

@Getter
public abstract class TaskEvent extends BaseEvent {
    private final Long projectId;  // Loose reference
    private final String taskTitle;
    
    // Optional: You can include projectName if you have it cheap, 
    // useful for email subjects without a DB lookup.
    private final String projectName; 

    protected TaskEvent(User actor, String ipAddress, Long projectId, String projectName, String taskTitle) {
        super(actor, ipAddress);
        this.projectId = projectId;
        this.projectName = projectName;
        this.taskTitle = taskTitle;
    }
}