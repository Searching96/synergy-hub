package com.synergyhub.organization.dto;

import com.synergyhub.organization.entity.SsoProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoProviderConfigRequest {

    @NotNull(message = "Provider type is required")
    private SsoProvider.ProviderType providerType;

    @NotBlank(message = "Provider name is required")
    private String providerName;

    // SAML specific
    private String entityId;
    private String ssoUrl;
    private String sloUrl;
    private String x509Certificate;

    // OIDC/OAuth2 specific
    private String clientId;
    private String clientSecret;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String jwksUri;
}