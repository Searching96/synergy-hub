package com.synergyhub.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_codes", indexes = {
    @Index(name = "idx_backup_user_used", columnList = "user_id, used")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // Prevent circular reference in serialization
    private User user;

    // Stores the HASHED code (e.g., BCrypt string). 
    // Never store the plain text version.
    @NotBlank
    @Column(nullable = false, length = 255)
    @JsonIgnore // Security: Never return even the hash in API responses
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;
}