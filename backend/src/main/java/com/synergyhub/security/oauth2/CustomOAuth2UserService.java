package com.synergyhub.security.oauth2;

import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.AuthProvider;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            // Ensure error code is not null/empty
            String msg = ex.getMessage() != null ? ex.getMessage() : "An error occurred while processing OAuth2 login";
            throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("login_failure"), msg, ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        log.info("Processing OAuth2 login for provider: [{}]", registrationId);
        
        String email = oAuth2User.getAttribute("email");
        log.info("OAuth2 attributes found. Email: [{}], Name: [{}]", email, oAuth2User.getAttribute("name"));
        
        if ((email == null || email.isEmpty()) && "github".equalsIgnoreCase(registrationId)) {
            email = fetchGitHubEmail(oAuth2UserRequest);
        }

        if (email == null || email.isEmpty()) {
            // Fallback: Generate a unique internal email if we really can't find one
            String login = oAuth2User.getAttribute("login");
            if (login != null) {
                email = login + "@users.noreply.github.com";
                log.warn("Could not fetch real email. Falling back to generated email: {}", email);
            } else {
                log.error("Email AND Login not found in OAuth2 attributes. Attributes: {}", oAuth2User.getAttributes());
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
            }
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Pass the possibly resolved email back (though attributes are immutable, we use the resolved 'email' var)
            user = updateExistingUser(user, oAuth2User, registrationId, email); 
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2User, email);
        }

        // Return OAuth2User (DefaultOAuth2User) instead of UserPrincipal
        // The SuccessHandler will fetch the User from DB using the email
        String userNameAttributeName = oAuth2UserRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        
        // We might want to inject the resolved email back into attributes if possible, but DefaultOAuth2User attributes are final.
        // However, the SuccessHandler fetches by email from the Principal. 
        // Wait, SuccessHandler gets email from principal.getAttribute("email").
        // If we don't update the principal's attributes, SuccessHandler will fail to find the email!
        // So we must return a new DefaultOAuth2User with the resolved email in attributes.
        
        Map<String, Object> attributes = new java.util.HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", email);

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                userNameAttributeName
        );
    }
    
    private String fetchGitHubEmail(OAuth2UserRequest request) {
        String token = request.getAccessToken().getTokenValue();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                return emails.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                    .map(e -> (String) e.get("email"))
                    .findFirst()
                    .orElse(emails.stream()
                        .map(e -> (String) e.get("email"))
                        .findFirst()
                        .orElse(null));
            }
        } catch (Exception e) {
            log.error("Failed to fetch emails from GitHub", e);
        }
        return null;
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User, String resolvedEmail) {
        AuthProvider provider = AuthProvider.valueOf(
                oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
        String name = oAuth2User.getAttribute("name");
        String imageUrl = oAuth2User.getAttribute("picture");

        if (name == null) name = resolvedEmail.split("@")[0];
        if (imageUrl == null && oAuth2User.getAttribute("avatar_url") != null) {
            imageUrl = oAuth2User.getAttribute("avatar_url");
        }

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(oAuth2User.getName());
        user.setName(name);
        user.setEmail(resolvedEmail);
        user.setImageUrl(imageUrl);
        user.setEmailVerified(true);
        user.setTwoFactorEnabled(false);
        user.setPassword(UUID.randomUUID().toString()); // Dummy password

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2User oAuth2User, String registrationId, String resolvedEmail) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        
        if (!existingUser.getProvider().equals(provider)) {
            log.info("Updating existing user provider to {}", registrationId);
            existingUser.setProvider(provider);
        }
        
        String name = oAuth2User.getAttribute("name");
        String imageUrl = oAuth2User.getAttribute("picture");
        if (imageUrl == null && oAuth2User.getAttribute("avatar_url") != null) {
            imageUrl = oAuth2User.getAttribute("avatar_url");
        }

        boolean changed = false;
        if (name != null && !name.equals(existingUser.getName())) {
            existingUser.setName(name);
            changed = true;
        }
        if (imageUrl != null && !imageUrl.equals(existingUser.getImageUrl())) {
            existingUser.setImageUrl(imageUrl);
            changed = true;
        }

        if (changed || !existingUser.getProvider().equals(provider)) {
            return userRepository.save(existingUser);
        }
        return existingUser;
    }
}
