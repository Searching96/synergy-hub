package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class AccountUnlockedEvent extends UserEvent {
    private final String reason;
    
    public AccountUnlockedEvent(User user, String ipAddress, String reason) {
        super(user, ipAddress);
        this.reason = reason;
    }
}