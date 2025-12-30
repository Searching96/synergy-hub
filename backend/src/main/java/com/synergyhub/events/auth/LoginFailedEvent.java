package com.synergyhub.events.auth;

import com.synergyhub.events.BaseEvent;

import lombok.Getter;

@Getter
public class LoginFailedEvent extends BaseEvent {
    private final String email;
    private final String userAgent;
    private final String reason;

    public LoginFailedEvent(String email, String ipAddress, String userAgent, String reason) {
        super(ipAddress);
        this.email = email;
        this.userAgent = userAgent;
        this.reason = reason;
    }
}