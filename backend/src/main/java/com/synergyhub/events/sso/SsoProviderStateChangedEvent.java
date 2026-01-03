package com.synergyhub.events.sso;

import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

/**
 * Event published when an SSO provider is enabled/disabled.
 * Used for audit logging state changes.
 */
@Getter
public class SsoProviderStateChangedEvent extends BaseEvent {
    private final SsoProvider provider;
    private final Boolean newState;

    public SsoProviderStateChangedEvent(SsoProvider provider, Boolean newState, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.provider = provider;
        this.newState = newState;
    }
}
