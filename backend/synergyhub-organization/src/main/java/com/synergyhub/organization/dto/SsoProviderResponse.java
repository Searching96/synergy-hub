package com.synergyhub.organization.dto;

import com.synergyhub.organization.entity.SsoProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoProviderResponse {
    private Integer ssoProviderId;
    private Integer organizationId;
    private SsoProvider.ProviderType providerType;
    private String providerName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}