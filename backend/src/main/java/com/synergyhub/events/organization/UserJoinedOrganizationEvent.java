package com.synergyhub.events.organization;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserJoinedOrganizationEvent extends ApplicationEvent {
    
    private final Organization organization;
    private final User user;
    private final String ipAddress;
    
    public UserJoinedOrganizationEvent(Organization organization, User user, String ipAddress) {
        super(organization);
        this.organization = organization;
        this.user = user;
        this.ipAddress = ipAddress;
    }
}
