package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.LoginRequest;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.exception.TwoFactorAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j


public class LoginService {

    private final SessionService sessionService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserMapper userMapper;
    private final AuthenticationStrategy authenticationStrategy;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            User user = authenticationStrategy.authenticate(request, ipAddress, userAgent);


            // Handle 2FA
            if (user.getTwoFactorEnabled() && (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty())) {
                String temporaryToken = sessionService.getJwtTokenProvider().generateTemporaryToken(user.getId(), user.getEmail());
                return LoginResponse.builder()
                        .twoFactorRequired(true)
                        .twoFactorToken(temporaryToken)
                        .build();
            }
            if (user.getTwoFactorEnabled() && request.getTwoFactorCode() != null && !request.getTwoFactorCode().isEmpty()) {
                // You may need to inject TwoFactorAuthService directly if not already present
                boolean isValid = twoFactorAuthService.verifyCode(user.getEmail(), request.getTwoFactorCode(), ipAddress);
                if (!isValid) {
                    throw new TwoFactorAuthenticationException("Invalid two-factor authentication code");
                }
            }

            authenticationStrategy.handlePostLogin(user, ipAddress, userAgent);

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            // Persist last login (still needs UserRepository, can be moved to strategy if desired)
            // userRepository.save(user); // Uncomment if needed


                // Create session and generate JWT token
                String token = sessionService.createSession(user, userAgent, ipAddress);

                // Map user response
                UserResponse userResponse = userMapper.toUserResponse(user);

                return LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(sessionService.getJwtTokenProvider().getExpirationMs() / 1000)
                    .user(userResponse)
                    .twoFactorRequired(false)
                    .build();

        } catch (BadCredentialsException e) {
            authenticationStrategy.handleFailedLogin(request.getEmail(), ipAddress, userAgent, "Invalid credentials");
            throw e;
        }
    }
    /**
     * Refresh access token using a valid refresh token
     * @param refreshToken The refresh token to validate
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return LoginResponse with new access token and optionally new refresh token
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken, String ipAddress, String userAgent) {
        log.debug("Refreshing token for IP: {}", ipAddress);
        
        try {
            // Validate refresh token and get user details
            Integer userId = sessionService.getJwtTokenProvider().getUserIdFromToken(refreshToken);
            String email = sessionService.getJwtTokenProvider().getEmailFromToken(refreshToken);
            
            if (userId == null || email == null) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            
            // Verify token is not expired and is valid
            if (!sessionService.getJwtTokenProvider().validateToken(refreshToken)) {
                throw new BadCredentialsException("Refresh token expired or invalid");
            }
            
            // Get user through authentication strategy
            User user = authenticationStrategy.getUserByEmail(email);
            
            if (user == null || !user.isActive()) {
                throw new BadCredentialsException("User not found or inactive");
            }
            
            // Generate new access token (and optionally new refresh token)
            String newAccessToken = sessionService.createSession(user, userAgent, ipAddress);
            String newRefreshToken = sessionService.getJwtTokenProvider().generateRefreshToken(user.getId(), email);
            
            // Map user response
            UserResponse userResponse = userMapper.toUserResponse(user);
            
            log.info("Token refreshed successfully for user: {}", email);
            
            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(sessionService.getJwtTokenProvider().getExpirationMs() / 1000)
                    .user(userResponse)
                    .twoFactorRequired(false)
                    .build();
                    
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid or expired refresh token", e);
        }
    }
}
