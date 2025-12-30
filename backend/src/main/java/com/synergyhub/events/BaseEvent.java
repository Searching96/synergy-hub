package com.synergyhub.events;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

import java.time.Instant;

@Getter
public abstract class BaseEvent {
    private final User actor;       // Nullable (for anonymous events)
    private final String ipAddress;
    private final long timestamp;

    // Constructor 1: For Authenticated Events (ProjectEvent, TaskEvent, UserEvent)
    protected BaseEvent(User actor, String ipAddress) {
        this.actor = actor;
        this.ipAddress = ipAddress;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Constructor 2: For Anonymous Events (LoginFailedEvent)
    protected BaseEvent(String ipAddress) {
        this.actor = null;
        this.ipAddress = ipAddress;
        this.timestamp = Instant.now().toEpochMilli();
    }
}