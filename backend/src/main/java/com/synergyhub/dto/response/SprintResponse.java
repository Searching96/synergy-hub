package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.SprintStatus;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {

    private Long id;
    private String name;
    private String goal;

    private Long projectId;
    private String projectName;

    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;

    private Integer totalTasks;
    private Integer completedTasks;
    private Integer inProgressTasks;

    private Double completionPercentage;
    private Integer remainingDays;
}
