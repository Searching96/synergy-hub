package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class TwoFactorBackupCodeUsedEvent extends UserEvent {
    
    public TwoFactorBackupCodeUsedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}