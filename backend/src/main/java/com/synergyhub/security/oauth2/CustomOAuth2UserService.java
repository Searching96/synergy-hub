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

import java.util.Collections;
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
        String email = oAuth2User.getAttribute("email");
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2User, registrationId);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2User);
        }

        // Return OAuth2User (DefaultOAuth2User) instead of UserPrincipal
        // The SuccessHandler will fetch the User from DB using the email
        String userNameAttributeName = oAuth2UserRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        
        return new DefaultOAuth2User(
                Collections.emptyList(),
                oAuth2User.getAttributes(),
                userNameAttributeName
        );
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        AuthProvider provider = AuthProvider.valueOf(
                oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
        String name = oAuth2User.getAttribute("name");
        String email = oAuth2User.getAttribute("email");
        String imageUrl = oAuth2User.getAttribute("picture");

        if (name == null) name = email.split("@")[0];
        if (imageUrl == null && oAuth2User.getAttribute("avatar_url") != null) {
            imageUrl = oAuth2User.getAttribute("avatar_url");
        }

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(oAuth2User.getName());
        user.setName(name);
        user.setEmail(email);
        user.setImageUrl(imageUrl);
        user.setEmailVerified(true);
        user.setPassword(UUID.randomUUID().toString()); // Dummy password

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2User oAuth2User, String registrationId) {
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
