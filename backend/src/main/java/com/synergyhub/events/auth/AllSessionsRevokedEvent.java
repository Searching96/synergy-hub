package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;

public class AllSessionsRevokedEvent extends UserEvent {
    public AllSessionsRevokedEvent(User user, String ipAddress) {
        super(user, ipAddress);
    }
}