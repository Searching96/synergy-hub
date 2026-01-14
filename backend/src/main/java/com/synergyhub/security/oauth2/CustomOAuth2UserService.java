package com.synergyhub.security.oauth2;

import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.AuthProvider;
import com.synergyhub.domain.enums.RoleType;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            // Throwing an instance of OAuth2AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        // Google uses "sub", GitHub uses "id". We need a unified way or just check attributes.
        // Usually email is the best identifier for enterprise apps.
        String email = oAuth2User.getAttribute("email");
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(registrationId.toUpperCase()))) {
                // If user registered with local/other provider, we might want to link accounts or throw error.
                // For simplicity here, we update the provider to allow social login or logging
                log.info("Updating existing user provider to {}", registrationId);
                user.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
            }
            user = updateExistingUser(user, oAuth2User);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2User);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        AuthProvider provider = AuthProvider.valueOf(
                oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
        String name = oAuth2User.getAttribute("name");
        String email = oAuth2User.getAttribute("email");
        String imageUrl = oAuth2User.getAttribute("picture"); // Google specific, might differ for GitHub (avatar_url)

        if (name == null) name = email.split("@")[0]; // Fallback
        if (imageUrl == null && oAuth2User.getAttribute("avatar_url") != null) {
            imageUrl = oAuth2User.getAttribute("avatar_url");
        }

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(oAuth2User.getName()); // Subject/ID
        user.setName(name);
        user.setEmail(email);
        user.setImageUrl(imageUrl);
        user.setEmailVerified(true); // OAuth2 emails are trusted
        user.setPassword(UUID.randomUUID().toString()); // Dummy password

        User savedUser = userRepository.save(user);
        
        // Add to default organization if needed? 
        // Or leave them org-less until they join one. 
        // For now, let's leave them org-less but ensure they have basic setup if we have a default org mechanism.
        
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2User oAuth2User) {
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

        if (changed) {
            return userRepository.save(existingUser);
        }
        return existingUser;
    }
}
