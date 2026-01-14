package com.synergyhub.dto.response;

import lombok.Data;
import java.util.Set;
import lombok.Builder;

@Data
@Builder
public class UserOrganizationResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private Long organizationId;
    private String organizationName;
    private Set<String> roles;
    private Set<String> permissions;
    private boolean hasOrganization;
}
