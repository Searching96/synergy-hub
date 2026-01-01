package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class TwoFactorDisabledEvent extends UserEvent {
    public TwoFactorDisabledEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}