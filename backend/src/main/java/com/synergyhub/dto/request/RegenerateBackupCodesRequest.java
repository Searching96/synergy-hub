package com.synergyhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegenerateBackupCodesRequest {
    @NotBlank(message = "Verification code is required")
    private String verificationCode; 
}