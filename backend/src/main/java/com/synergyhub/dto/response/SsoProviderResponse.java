package com.synergyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.synergyhub.domain.enums.SsoProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for SSO provider configuration.
 * Sensitive fields (client_secret) are excluded from JSON output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoProviderResponse {
    
    private Long id;
    
    private SsoProviderType providerType;
    
    private String providerName;
    
    private String clientId;
    
    @JsonIgnore
    private String clientSecret; // Never exposed in responses
    
    private String metadataUrl;
    
    private Boolean enabled;
    
    private LocalDateTime createdAt;
}
