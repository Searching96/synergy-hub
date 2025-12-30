package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class RegistrationCompletedEvent extends UserEvent {
    public RegistrationCompletedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}
