package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class PasswordResetCompletedEvent extends UserEvent {
    public PasswordResetCompletedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}