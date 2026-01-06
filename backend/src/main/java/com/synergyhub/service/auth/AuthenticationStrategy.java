package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.LoginRequest;

/**
 * Strategy interface for authentication and session creation, to decouple LoginService from direct entity usage.
 */
public interface AuthenticationStrategy {
    User authenticate(LoginRequest request, String ipAddress, String userAgent);
    void handlePostLogin(User user, String ipAddress, String userAgent);
    void handleFailedLogin(String email, String ipAddress, String userAgent, String reason);
    User getUserByEmail(String email);
}
