package com.synergyhub.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String description;

    @Email(message = "Invalid contact email format")
    private String contactEmail;

    private String websiteUrl;

    private String logoUrl;

    private Integer maxUsers;

    private Boolean ssoEnabled;

    private Boolean isActive;
}