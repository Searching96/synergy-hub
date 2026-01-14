package com.synergyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationResponse {
    
    private Long id;
    private String name;
    private String address;
    private String contactEmail; // Also adding this as it was in service but missing here
    private LocalDateTime createdAt;
    private Long userCount;
    private String inviteCode;
    private LocalDateTime inviteCodeExpiresAt;
}