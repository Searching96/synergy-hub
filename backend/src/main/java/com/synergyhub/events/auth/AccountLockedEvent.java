package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class AccountLockedEvent extends UserEvent {
    public AccountLockedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}