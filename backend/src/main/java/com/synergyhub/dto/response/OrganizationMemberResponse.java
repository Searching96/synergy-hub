package com.synergyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime joinedAt;
    private String status;
}
