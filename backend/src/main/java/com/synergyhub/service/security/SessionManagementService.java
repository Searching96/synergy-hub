package com.synergyhub.service.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    @Transactional
    public void logout(UserPrincipal userPrincipal, String token, String ipAddress) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();

        String tokenId = jwtTokenProvider.getTokenIdFromToken(token);
        userSessionRepository.revokeSessionByTokenId(tokenId);

        auditLogService.logLogout(user, ipAddress);
        log.info("User logged out: {}", user.getEmail());
    }

    @Transactional
    public void logoutAllDevices(UserPrincipal userPrincipal, String ipAddress) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();

        userSessionRepository.revokeAllUserSessions(user);

        auditLogService.logSessionRevoked(user, ipAddress, "User logged out from all devices");
        log.info("User logged out from all devices: {}", user.getEmail());
    }
}