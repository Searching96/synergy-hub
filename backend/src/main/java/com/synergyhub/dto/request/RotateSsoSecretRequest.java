package com.synergyhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rotating SSO provider client secret.
 * Used for security rotation without full provider update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotateSsoSecretRequest {
    
    @NotBlank(message = "New client secret is required")
    @Size(min = 1, max = 512, message = "Client secret must not exceed 512 characters")
    private String newClientSecret;
}
