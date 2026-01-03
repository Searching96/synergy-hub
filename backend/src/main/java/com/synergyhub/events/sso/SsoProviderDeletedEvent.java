package com.synergyhub.events.sso;

import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

/**
 * Event published when an SSO provider is deleted.
 * Used for audit logging deletions.
 */
@Getter
public class SsoProviderDeletedEvent extends BaseEvent {
    private final SsoProvider provider;

    public SsoProviderDeletedEvent(SsoProvider provider, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.provider = provider;
    }
}
