package com.synergyhub.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_token", columnList = "token_id"),
        @Index(name = "idx_user_active", columnList = "user_id, revoked, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @Column(name = "token_id", nullable = false, unique = true, length = 255)
    private String tokenId; // JWT jti claim

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_accessed_at", nullable = false, updatable = true)
    private LocalDateTime lastAccessedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    public boolean isValid() {
        return !revoked && LocalDateTime.now().isBefore(expiresAt);
    }
}
