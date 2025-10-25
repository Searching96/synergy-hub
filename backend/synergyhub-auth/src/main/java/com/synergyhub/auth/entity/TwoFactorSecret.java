package com.synergyhub.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "two_factor_secrets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSecret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "secret_id")
    private Integer secretId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 255)
    private String secret;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;  // JSON array

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}