package com.synergyhub.events.system;

import com.synergyhub.events.BaseEvent;
import lombok.Getter;

@Getter
public class LoginAttemptsCleanupEvent extends BaseEvent {
    private final int daysRetained;
    private final long deletedCount;
    
    public LoginAttemptsCleanupEvent(int daysRetained, long deletedCount, String ipAddress) {
        super(null, ipAddress); // System operation, no specific user
        this.daysRetained = daysRetained;
        this.deletedCount = deletedCount;
    }
}