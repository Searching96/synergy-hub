package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class TwoFactorBackupCodeRegeneratedEvent extends UserEvent {
    public TwoFactorBackupCodeRegeneratedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}