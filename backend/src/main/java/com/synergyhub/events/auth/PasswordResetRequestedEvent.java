package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class PasswordResetRequestedEvent extends UserEvent {
    private final String token;

    public PasswordResetRequestedEvent(User user, String token, String ipAddress) {
        super(user, ipAddress);
        this.token = token;
    }
}