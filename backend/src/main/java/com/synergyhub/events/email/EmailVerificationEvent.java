package com.synergyhub.events.email;

import com.synergyhub.domain.entity.User;
import com.synergyhub.events.auth.UserEvent;

import lombok.Getter;

@Getter
public class EmailVerificationEvent extends UserEvent {
    private final String token;

    public EmailVerificationEvent(User user, String token, String ipAddress) {
        super(user, ipAddress);
        this.token = token;
    }
}