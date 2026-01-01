package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class UserCreatedEvent extends UserEvent {
    public UserCreatedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}