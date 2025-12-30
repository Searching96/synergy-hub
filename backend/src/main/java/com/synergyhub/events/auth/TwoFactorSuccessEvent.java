package com.synergyhub.events.auth;

import com.synergyhub.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorSuccessEvent {
    private final User user;
    private final String ipAddress;
}
