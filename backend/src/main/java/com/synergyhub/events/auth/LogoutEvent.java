package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class LogoutEvent extends UserEvent {
    public LogoutEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}