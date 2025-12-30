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
}