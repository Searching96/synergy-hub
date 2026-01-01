package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class TwoFactorSetupInitiatedEvent extends UserEvent {
    public TwoFactorSetupInitiatedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}