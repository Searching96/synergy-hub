package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class TwoFactorDisableFailedEvent extends UserEvent {
    public TwoFactorDisableFailedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}