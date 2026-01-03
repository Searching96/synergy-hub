package com.synergyhub.events.organization;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;

import lombok.Getter;

@Getter
public abstract class OrganizationEvent extends BaseEvent {
    private final Organization organization;

    protected OrganizationEvent(Organization organization, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.organization = organization;
    }
}
