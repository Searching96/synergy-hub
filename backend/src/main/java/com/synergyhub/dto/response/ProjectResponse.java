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

    private Long id;
    private String name;
    private String description;
    private Long organizationId;
    private String organizationName;
    private ProjectLeadResponse projectLead;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer memberCount;
    private Integer taskCount;
    private Integer completedTaskCount;
    private Long teamId;
    private String teamName;
}
