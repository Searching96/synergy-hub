package com.synergyhub.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class JoinOrganizationRequest {
    @NotNull
    private String inviteCode;
}
