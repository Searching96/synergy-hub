package com.synergyhub.auth.security;

import com.synergyhub.auth.entity.Permission;
import com.synergyhub.auth.entity.Role;
import com.synergyhub.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {
    private final Integer userId;
    private final String email;
    private final String password;
    private final Integer organizationId;
    private final boolean accountLocked;
    private final boolean emailVerified;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.organizationId = user.getOrganizationId();
        this.accountLocked = !user.isAccountNonLocked();
        this.emailVerified = user.getEmailVerified();

        Set<GrantedAuthority> auths = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                // Add role
                auths.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase().replace(' ', '_')));
                
                // Add permissions
                if (role.getPermissions() != null) {
                    for (Permission perm : role.getPermissions()) {
                        auths.add(new SimpleGrantedAuthority(perm.getName()));
                    }
                }
            }
        }
        this.authorities = auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
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