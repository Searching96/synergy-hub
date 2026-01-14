package com.synergyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    
    private Long id;
    private String name;
    private String email;
    private String imageUrl;
    private Long organizationId;
    private String organizationName;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private Set<String> roles;
    private Set<String> permissions;
}