package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.SprintStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintDetailResponse {

    private Integer id;
    private String name;
    private String goal;

    private Integer projectId;
    private String projectName;

    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;

    private List<TaskSummaryResponse> tasks;
    private SprintMetrics metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintMetrics {
        private Integer totalTasks;
        private Integer completedTasks;
        private Integer inProgressTasks;
        private Integer todoTasks;
        private Integer blockedTasks;

        private Integer totalStoryPoints;
        private Integer completedStoryPoints;

        private Double completionPercentage;
        private Double velocityPercentage;

        private Integer totalDays;
        private Integer elapsedDays;
        private Integer remainingDays;

        private Boolean isOverdue;
    }
}
