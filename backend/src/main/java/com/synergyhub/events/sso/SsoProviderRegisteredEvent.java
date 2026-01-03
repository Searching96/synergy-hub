package com.synergyhub.events.sso;

import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

/**
 * Event published when a new SSO provider is registered.
 * Used for audit logging.
 */
@Getter
public class SsoProviderRegisteredEvent extends BaseEvent {
    private final SsoProvider provider;

    public SsoProviderRegisteredEvent(SsoProvider provider, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.provider = provider;
    }
}
