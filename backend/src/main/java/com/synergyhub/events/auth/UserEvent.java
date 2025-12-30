package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;

import lombok.Getter;

@Getter
public abstract class UserEvent extends BaseEvent {
    private final User user;

    protected UserEvent(User user, String ipAddress) {
        super(user, ipAddress);
        this.user = user;
    }
}