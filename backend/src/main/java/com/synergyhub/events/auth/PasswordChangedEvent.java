package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class PasswordChangedEvent extends UserEvent {
    public PasswordChangedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}
