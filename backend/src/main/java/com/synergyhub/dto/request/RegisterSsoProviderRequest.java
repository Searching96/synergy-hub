package com.synergyhub.dto.request;

import com.synergyhub.domain.enums.SsoProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a new SSO provider.
 * Accepts provider type, name, credentials, and metadata URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSsoProviderRequest {
    
    @NotNull(message = "Provider type is required")
    private SsoProviderType providerType;
    
    @NotBlank(message = "Provider name is required")
    @Size(min = 3, max = 100, message = "Provider name must be between 3 and 100 characters")
    private String providerName;
    
    @NotBlank(message = "Client ID is required")
    @Size(min = 1, max = 255, message = "Client ID must not exceed 255 characters")
    private String clientId;
    
    @NotBlank(message = "Client secret is required")
    @Size(min = 1, max = 512, message = "Client secret must not exceed 512 characters")
    private String clientSecret;
    
    @Size(max = 500, message = "Metadata URL must not exceed 500 characters")
    private String metadataUrl;
}
