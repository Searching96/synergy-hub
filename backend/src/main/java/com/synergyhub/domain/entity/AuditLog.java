package com.synergyhub.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // ✅ Can be null for system events
    private User user;

    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type must not exceed 100 characters")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // ✅ Consistent with your service

    @Size(max = 10000, message = "Event details must not exceed 10000 characters")
    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails; // ✅ Consistent with your service

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "project_id") // ✅ Context for project-related events
    private Integer projectId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}