package com.synergyhub.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_email_time", columnList = "email, attempted_at"),
    @Index(name = "idx_ip_time", columnList = "ip_address, attempted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Integer id;
    
    @Column(nullable = false, length = 100)
    @Email
    private String email;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(nullable = false)
    private Boolean success;
    
    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;
}