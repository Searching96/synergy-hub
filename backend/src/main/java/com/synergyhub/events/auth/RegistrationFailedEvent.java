package com.synergyhub.events.auth;

import lombok.Getter;

@Getter
public class RegistrationFailedEvent extends com.synergyhub.events.BaseEvent {
    private final String email;
    private final String reason;
    
    public RegistrationFailedEvent(String email, String ipAddress, String reason) {
        super(null, ipAddress); // No user yet
        this.email = email;
        this.reason = reason;
    }
}