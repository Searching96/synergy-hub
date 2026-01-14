package com.synergyhub.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.synergyhub.security.AttributeEncryptor; // Reuse your existing encryptor
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "two_factor_secrets", indexes = {
    @Index(name = "idx_2fa_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"secret", "user"}) // 1. Prevent Log Leaks & Circular Refs
public class TwoFactorSecret {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "secret_id")
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    // --- SECURITY FIX APPLIED HERE ---
    @NotBlank
    @Column(nullable = false, length = 512) // Increased length for ciphertext
    @Convert(converter = AttributeEncryptor.class) // 2. Encryption at rest
    @JsonIgnore // 3. Prevent JSON serialization
    private String secret;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}