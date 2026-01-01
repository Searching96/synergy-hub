package com.synergyhub.service.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.events.auth.AllSessionsRevokedEvent;
import com.synergyhub.events.auth.LogoutEvent;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void logout(UserPrincipal userPrincipal, String token, String ipAddress) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String tokenId = jwtTokenProvider.getTokenIdFromToken(token);
        userSessionRepository.revokeSessionByTokenId(tokenId);

        // ✅ Publish logout event
        eventPublisher.publishEvent(new LogoutEvent(user, ipAddress));
        
        log.info("User logged out: {}", user.getEmail());
    }

    @Transactional
    public void logoutAllDevices(UserPrincipal userPrincipal, String ipAddress) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        userSessionRepository.revokeAllUserSessions(user);

        // ✅ Publish all sessions revoked event
        eventPublisher.publishEvent(new AllSessionsRevokedEvent(user, ipAddress));
        
        log.info("User logged out from all devices: {}", user.getEmail());
    }
}