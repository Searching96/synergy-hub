package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class EmailVerifiedEvent extends UserEvent {
    public EmailVerifiedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}