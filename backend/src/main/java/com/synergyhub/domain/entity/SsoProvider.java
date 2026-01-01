package com.synergyhub.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.synergyhub.domain.enums.SsoProviderType;
import com.synergyhub.security.AttributeEncryptor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sso_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoProvider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private SsoProviderType providerType;
    
    @NotBlank
    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;
    
    @Column(name = "client_id", length = 255)
    @NotBlank
    private String clientId;

    // --- SECURITY FIX APPLIED HERE ---
    @Column(name = "client_secret", length = 512) // INCREASED LENGTH for ciphertext
    @Convert(converter = AttributeEncryptor.class) // 1. Encryption at rest
    @NotBlank
    @JsonIgnore // 2. Prevent JSON serialization
    private String clientSecret;
    
    @Column(name = "metadata_url", length = 500)
    private String metadataUrl;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SsoUserMapping> userMappings = new HashSet<>();
}