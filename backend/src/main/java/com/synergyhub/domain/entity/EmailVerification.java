package com.synergyhub.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications", indexes = {
    @Index(name = "idx_token", columnList = "token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"token", "user"}) // 1. Prevent Log Leaks & Circular Refs
public class EmailVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // --- SECURITY FIX APPLIED HERE ---
    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    @JsonIgnore // 2. Prevent JSON serialization
    private String token;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
    
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
    
    public boolean isValid() {
        return !verified && !isExpired();
    }
}