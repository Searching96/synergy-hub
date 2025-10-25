package com.synergyhub.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    @Column(name = "sso_provider_id")
    private Integer ssoProviderId;

    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderType providerType;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(name = "sso_url", length = 500)
    private String ssoUrl;

    @Column(name = "slo_url", length = 500)
    private String sloUrl;

    @Column(name = "x509_certificate", columnDefinition = "TEXT")
    private String x509Certificate;

    @Column(name = "client_id", length = 255)
    private String clientId;

    @Column(name = "client_secret", length = 500)
    private String clientSecret;

    @Column(name = "authorization_endpoint", length = 500)
    private String authorizationEndpoint;

    @Column(name = "token_endpoint", length = 500)
    private String tokenEndpoint;

    @Column(name = "userinfo_endpoint", length = 500)
    private String userinfoEndpoint;

    @Column(name = "jwks_uri", length = 500)
    private String jwksUri;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ProviderType {
        SAML,
        OIDC,
        OAUTH2
    }
}