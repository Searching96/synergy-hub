package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class LoginSuccessEvent extends UserEvent {
    private final String userAgent;

    public LoginSuccessEvent(User user, String ipAddress, String userAgent) {
        super(user, ipAddress);
        this.userAgent = userAgent;
    }
}