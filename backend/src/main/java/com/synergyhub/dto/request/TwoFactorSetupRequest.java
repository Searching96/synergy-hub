package com.synergyhub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class TwoFactorSetupRequest {
    // This request body can be empty as setup is initiated by authenticated user
    // Could add optional fields like preferredMethod (TOTP, SMS, Email) in future
}