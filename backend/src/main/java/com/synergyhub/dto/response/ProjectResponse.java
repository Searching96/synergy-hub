package com.synergyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Integer id;
    private String name;
    private String description;
    private Integer organizationId;
    private String organizationName;
    private UserResponse projectLead;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer memberCount;
}
