package com.synergyhub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing SSO provider.
 * Allows partial updates to provider configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSsoProviderRequest {
    
    @Size(min = 3, max = 100, message = "Provider name must be between 3 and 100 characters")
    private String providerName;
    
    @Size(min = 1, max = 255, message = "Client ID must not exceed 255 characters")
    private String clientId;
    
    @Size(min = 1, max = 512, message = "Client secret must not exceed 512 characters")
    private String clientSecret;
    
    @Size(max = 500, message = "Metadata URL must not exceed 500 characters")
    private String metadataUrl;
}
