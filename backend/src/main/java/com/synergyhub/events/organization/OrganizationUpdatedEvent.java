package com.synergyhub.events.organization;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;

public class OrganizationUpdatedEvent extends OrganizationEvent {
    public OrganizationUpdatedEvent(Organization organization, User actor, String ipAddress) {
        super(organization, actor, ipAddress);
    }
}
