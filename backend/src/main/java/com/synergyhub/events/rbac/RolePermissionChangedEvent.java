package com.synergyhub.events.rbac;

import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

@Getter
public class RolePermissionChangedEvent extends BaseEvent {
    private final Role role;
    private final String changeType; // "ASSIGNED" or "REVOKED"
    private final java.util.Set<Long> permissionIds;

    public RolePermissionChangedEvent(
            Role role,
            String changeType,
            java.util.Set<Long> permissionIds,
            User actor,
            String ipAddress) {
        super(actor, ipAddress);
        this.role = role;
        this.changeType = changeType;
        this.permissionIds = permissionIds;
    }
}