package com.synergyhub.security.oauth2;

import com.synergyhub.domain.entity.User;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.service.auth.SessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        String targetUrl = determineTargetUrl(request, response, authentication);
        logger.info("Redirecting to targetUrl: " + targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        
        // Fetch the user from database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        // Generate tokens using existing methods
        String accessToken = tokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        
        // Create session in DB using existing signature
        sessionService.createSession(user, request.getHeader("User-Agent"), request.getRemoteAddr());

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();
    }
}
