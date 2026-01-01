package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class AccountLockedEvent extends UserEvent {
    private final int failedAttempts;
    private final int lockDurationMinutes;
    
    public AccountLockedEvent(User user, String ipAddress, int failedAttempts, int lockDurationMinutes) {
        super(user, ipAddress);
        this.failedAttempts = failedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }
}