package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class AccountForceLockedEvent extends UserEvent {
    private final User admin;
    private final String reason;
    
    public AccountForceLockedEvent(User user, User admin, String ipAddress, String reason) {
        super(user, ipAddress);
        this.admin = admin;
        this.reason = reason;
    }
}