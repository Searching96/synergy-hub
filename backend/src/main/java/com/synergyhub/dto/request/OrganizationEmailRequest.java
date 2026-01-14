package com.synergyhub.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Data
public class OrganizationEmailRequest {
    @NotNull
    @Email
    private String organizationEmail;
}
