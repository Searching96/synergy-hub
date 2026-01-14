package com.synergyhub.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
public class User {
    // Legacy compatibility: getUserId()
    public Long getUserId() {
        return this.id;
    }

    // Legacy compatibility: setOrganization (sets current context)
    public void setOrganization(Organization org) {
        this.currentContextOrganization = org;
    }

    // Legacy compatibility: getRoles() (aggregates all roles from memberships)
    public java.util.Set<Role> getRoles() {
        java.util.Set<Role> roles = new java.util.HashSet<>();
        if (this.memberships != null) {
            for (UserOrganization membership : this.memberships) {
                if (membership.getRole() != null) {
                    roles.add(membership.getRole());
                }
            }
        }
        return roles;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    // --- OAuth2 Provider Fields ---
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    @lombok.Builder.Default
    private com.synergyhub.domain.enums.AuthProvider provider = com.synergyhub.domain.enums.AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "image_url")
    private String imageUrl;

    // Setter for password (maps to passwordHash)
    public void setPassword(String password) {
        this.passwordHash = password;
    }

    // --- Multi-Org Memberships ---
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @lombok.Builder.Default
    private List<UserOrganization> memberships = new ArrayList<>();

    // --- Current Session Context (not persisted) ---
    @Transient
    private Organization currentContextOrganization;

    // --- Account Lock & Login Tracking ---
    @Column(name = "account_locked")
    @lombok.Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "lock_until")
    private java.time.LocalDateTime lockUntil;

    @Column(name = "failed_login_attempts")
    @lombok.Builder.Default
    private Integer failedLoginAttempts = 0; // keep as Integer, not an ID

    // --- Methods for AccountLockService compatibility ---
    public Boolean getAccountLocked() {
        return accountLocked != null && accountLocked;
    }
    public void setAccountLocked(Boolean locked) {
        this.accountLocked = locked;
    }
    public java.time.LocalDateTime getLockUntil() {
        return lockUntil;
    }
    public void setLockUntil(java.time.LocalDateTime until) {
        this.lockUntil = until;
    }
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts != null ? failedLoginAttempts : 0;
    }
    public void setFailedLoginAttempts(Integer attempts) {
        this.failedLoginAttempts = attempts;
    }
    public void incrementFailedAttempts() {
        this.failedLoginAttempts = getFailedLoginAttempts() + 1;
    }
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }
    public void lock(int minutes) {
        this.accountLocked = true;
        this.lockUntil = java.time.LocalDateTime.now().plusMinutes(minutes);
    }

    // --- Multi-Org Helpers ---
    /**
     * Add a membership for this user in an organization with a role.
     */
    public void addMembership(Organization org, Role role) {
        UserOrganization membership = new UserOrganization(this, org, role);
        this.memberships.add(membership);
        // Optionally: org.getMemberships().add(membership); // if bidirectional
    }

    /**
     * Returns the organization for the current context, or the only org if just one, or null.
     */
    public Organization getOrganization() {
        if (this.currentContextOrganization != null) {
            return this.currentContextOrganization;
        }
        if (this.memberships != null && !this.memberships.isEmpty()) {
            // Try to find marked primary
            for (UserOrganization membership : this.memberships) {
                if (Boolean.TRUE.equals(membership.getIsPrimary())) {
                    return membership.getOrganization();
                }
            }
            // Fallback to first one
            return this.memberships.get(0).getOrganization();
        }
        return null;
    }

    // Compatibility Helper for legacy code expecting 'getId()'
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}