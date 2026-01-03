package com.synergyhub.events.sso;

import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

/**
 * Event published when an SSO provider is updated.
 * Used for audit logging configuration changes.
 */
@Getter
public class SsoProviderUpdatedEvent extends BaseEvent {
    private final SsoProvider provider;

    public SsoProviderUpdatedEvent(SsoProvider provider, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.provider = provider;
    }
}
