package com.synergyhub.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCreateRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Slug must not exceed 50 characters")
    private String slug;

    private String description;

    @Email(message = "Invalid contact email format")
    private String contactEmail;

    private String websiteUrl;

    private Integer maxUsers;
}