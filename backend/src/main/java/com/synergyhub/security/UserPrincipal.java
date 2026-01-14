package com.synergyhub.security;

import com.synergyhub.domain.entity.Permission;
import com.synergyhub.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private Long id;
    private String name;
    private String email;
    private String password;
    private Long organizationId;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private Collection<? extends GrantedAuthority> authorities;
    
    public static UserPrincipal create(User user) {
        // Remove orgId logic based on user.getOrganization()
        return new UserPrincipal(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getPasswordHash(),
            null, // orgId is now context-driven
            user.getEmailVerified(),
            user.getTwoFactorEnabled(),
            List.of() // authorities will be context-driven
        );
    }

    // OAuth2 support: create with attributes
    public static UserPrincipal create(User user, java.util.Map<String, Object> attributes) {
        UserPrincipal principal = create(user);
        // Attributes are available via OAuth2User interface if needed
        return principal;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    public List<com.synergyhub.domain.entity.Role> getRoles() {
        // In a real implementation, roles should be loaded from the User entity or context
        // For now, return an empty list or fetch from context if available
        return java.util.Collections.emptyList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Long currentOrgId = OrganizationContext.getcurrentOrgIdOrNull();
        if (currentOrgId == null) {
            return java.util.Collections.emptyList();
        }
        // Context-aware role filtering
        return this.getRoles().stream()
            .filter(role -> role.getOrganization() != null && role.getOrganization().getId().equals(currentOrgId))
            .flatMap(role -> role.getPermissions().stream())
            .map(perm -> new SimpleGrantedAuthority(perm.getName()))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}