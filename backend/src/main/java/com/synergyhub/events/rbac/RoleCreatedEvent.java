package com.synergyhub.events.rbac;

import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

@Getter
public class RoleCreatedEvent extends BaseEvent {
    private final Role role;

    public RoleCreatedEvent(Role role, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.role = role;
    }
}
