package com.synergyhub.security;

import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return UserPrincipal.create(user);
    }
    
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        // Check if organization context is set
        if (OrganizationContext.hasCurrentOrgId()) {
            Long orgId = OrganizationContext.getcurrentOrgId();
            User user = userRepository.findByIdWithRolesAndPermissionsInOrganization(id, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
            return UserPrincipal.create(user);
        } else {
            User user = userRepository.findByIdWithRolesAndPermissions(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
            return UserPrincipal.create(user);
        }
    }
}