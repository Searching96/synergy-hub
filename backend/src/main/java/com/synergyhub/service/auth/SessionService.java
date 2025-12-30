package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserSession;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.repository.UserSessionRepository;
import com.synergyhub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository; // ✅ Added dependency
    private final JwtTokenProvider jwtTokenProvider;

    public JwtTokenProvider getJwtTokenProvider() {
        return jwtTokenProvider;
    }

    @Transactional
    public String createSession(User user, String userAgent, String ipAddress) {
        String token = jwtTokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
        String tokenId = jwtTokenProvider.getTokenIdFromToken(token);

        UserSession session = UserSession.builder()
                .user(user)
                .tokenId(tokenId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationMs() / 1000))
                .revoked(false)
                .build();
        userSessionRepository.save(session);
        return token;
    }

    /**
     * List all active sessions for a user by User ID
     */
    @Transactional(readOnly = true)
    public List<UserSession> listActiveSessions(Integer userId) {
        User user = getUserById(userId);
        return userSessionRepository.findActiveSessionsByUser(user, LocalDateTime.now());
    }

    /**
     * Revoke a session by tokenId (logout from a specific device)
     * Includes OWNERSHIP CHECK to prevent IDOR attacks.
     */
    @Transactional
    public void revokeSession(String tokenId, Integer userId) {
        // 1. Fetch the session
        UserSession session = userSessionRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "tokenId", tokenId));

        // 2. Verify ownership
        if (!session.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to revoke this session");
        }

        // 3. Revoke
        session.setRevoked(true);
        userSessionRepository.save(session);
    }

    /**
     * Revoke all sessions for a user (force logout everywhere)
     */
    @Transactional
    public void revokeAllSessions(Integer userId) {
        User user = getUserById(userId);
        userSessionRepository.revokeAllUserSessions(user);
    }

    /**
     * Cleanup expired and revoked sessions
     */
    @Transactional
    public void cleanupExpiredAndRevokedSessions() {
        userSessionRepository.cleanupExpiredAndRevokedSessions(LocalDateTime.now());
    }

    // Helper to fetch user or throw exception
    private User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}