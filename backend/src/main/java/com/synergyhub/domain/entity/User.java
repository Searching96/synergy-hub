package com.synergyhub.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_organization", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 1. Exclude sensitive data from logs
// 2. Exclude relationships to prevent LazyInitException and StackOverflowError
@ToString(exclude = {"passwordHash", "organization", "roles", "sessions"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore // Prevent serialization of the entire parent Org object
    @NotNull
    private Organization organization;
    
    @Column(nullable = false, length = 100)
    @NotBlank
    private String name;
    
    @Column(nullable = false, unique = true, length = 100)
    @Email
    private String email;
    
    // --- SECURITY FIX APPLIED HERE ---
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;
    
    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;
    
    @Column(name = "lock_until")
    private LocalDateTime lockUntil;
    
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    @JsonIgnore // Usually better to expose roles via a specific DTO field, not the raw entity list
    private Set<Role> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore // Critical: Prevent serializing all historical sessions when fetching a user
    private Set<UserSession> sessions = new HashSet<>();
    
    // Helper methods remain unchanged
    public boolean isAccountNonLocked() {
        if (!accountLocked) {
            return true;
        }
        
        if (lockUntil != null && LocalDateTime.now().isAfter(lockUntil)) {
            // Auto-unlock if lock period has expired
            accountLocked = false;
            lockUntil = null;
            failedLoginAttempts = 0;
            return true;
        }
        
        return false;
    }
    
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }
    
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }
    
    public void lock(int durationMinutes) {
        this.accountLocked = true;
        this.lockUntil = LocalDateTime.now().plusMinutes(durationMinutes);
    }
}