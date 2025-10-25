package com.synergyhub.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto {
    private Integer userId;
    private String name;
    private String email;
    private Integer organizationId;
    private Set<String> roles;
    private Set<String> permissions;
    private Boolean twoFactorEnabled;
    private Boolean emailVerified;
}