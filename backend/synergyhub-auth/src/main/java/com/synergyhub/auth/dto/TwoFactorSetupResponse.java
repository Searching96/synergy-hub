package com.synergyhub.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private List<String> backupCodes;
}