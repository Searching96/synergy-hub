package com.synergyhub.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private Integer organizationId;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String websiteUrl;
    private String contactEmail;
    private Integer maxUsers;
    private Boolean ssoEnabled;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}