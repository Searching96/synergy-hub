package com.synergyhub.events.sso;

import com.synergyhub.domain.entity.SsoProvider;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;
import lombok.Getter;

/**
 * Event published when an SSO provider secret is rotated.
 * Marks CRITICAL event for compliance auditing.
 */
@Getter
public class SsoProviderSecretRotatedEvent extends BaseEvent {
    private final SsoProvider provider;

    public SsoProviderSecretRotatedEvent(SsoProvider provider, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.provider = provider;
    }
}
