package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Sprint;
import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.dto.request.CreateSprintRequest;
import com.synergyhub.dto.request.UpdateSprintRequest;
import com.synergyhub.dto.response.SprintDetailResponse;
import com.synergyhub.dto.response.SprintResponse;
import com.synergyhub.dto.response.TaskSummaryResponse;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface SprintMapper {

    // Basic Sprint Response
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "totalTasks", expression = "java(getTotalTasks(sprint))")
    @Mapping(target = "completedTasks", expression = "java(getCompletedTasks(sprint))")
    @Mapping(target = "inProgressTasks", expression = "java(getInProgressTasks(sprint))")
    @Mapping(target = "completionPercentage", expression = "java(calculateCompletionPercentage(sprint))")
    @Mapping(target = "remainingDays", expression = "java(calculateRemainingDays(sprint.getEndDate()))")
    SprintResponse toSprintResponse(Sprint sprint);

    // Detailed Sprint Response
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "tasks", expression = "java(mapTasksToSummary(sprint.getTasks()))")
    @Mapping(target = "metrics", expression = "java(calculateMetrics(sprint))")
    SprintDetailResponse toSprintDetailResponse(Sprint sprint);

    // Entity mapping
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Sprint toEntity(CreateSprintRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    void updateEntityFromRequest(UpdateSprintRequest request, @MappingTarget Sprint sprint);

    // List mapping
    List<SprintResponse> toSprintResponseList(List<Sprint> sprints);

    // ========== Helper Methods ==========

    default int getTotalTasks(Sprint sprint) {
        return sprint.getTasks() != null ? sprint.getTasks().size() : 0;
    }

    default int getCompletedTasks(Sprint sprint) {
        if (sprint.getTasks() == null) return 0;
        return (int) sprint.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
    }

    default int getInProgressTasks(Sprint sprint) {
        if (sprint.getTasks() == null) return 0;
        return (int) sprint.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS ||
                        task.getStatus() == TaskStatus.IN_REVIEW)
                .count();
    }

    default Double calculateCompletionPercentage(Sprint sprint) {
        int total = getTotalTasks(sprint);
        if (total == 0) return 0.0;

        int completed = getCompletedTasks(sprint);
        double percentage = (completed * 100.0) / total;
        return Math.round(percentage * 100.0) / 100.0;
    }

    default Integer calculateRemainingDays(LocalDate endDate) {
        if (endDate == null) return null;

        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return (int) Math.max(0, days);
    }

    default List<TaskSummaryResponse> mapTasksToSummary(List<Task> tasks) {
        if (tasks == null) return List.of();

        return tasks.stream()
                .map(task -> TaskSummaryResponse.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .status(task.getStatus())
                        .priority(task.getPriority())
                        .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                        .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                        .storyPoints(task.getStoryPoints())
                        .isOverdue(task.isOverdue())
                        .build())
                .collect(Collectors.toList());
    }

    default SprintDetailResponse.SprintMetrics calculateMetrics(Sprint sprint) {
        if (sprint.getTasks() == null || sprint.getTasks().isEmpty()) {
            return createEmptyMetrics(sprint);
        }

        List<Task> tasks = sprint.getTasks();
        int total = tasks.size();

        long completed = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        long inProgress = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS ||
                        task.getStatus() == TaskStatus.IN_REVIEW)
                .count();

        long todo = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.TO_DO)
                .count();

        long blocked = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.BLOCKED)
                .count();

        // Story points
        int totalStoryPoints = tasks.stream()
                .mapToInt(task -> task.getStoryPoints() != null ? task.getStoryPoints() : 0)
                .sum();

        int completedStoryPoints = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .mapToInt(task -> task.getStoryPoints() != null ? task.getStoryPoints() : 0)
                .sum();

        // Percentages
        double completionPercentage = total > 0 ? (completed * 100.0) / total : 0.0;
        double velocityPercentage = totalStoryPoints > 0 ?
                (completedStoryPoints * 100.0) / totalStoryPoints : 0.0;

        // Days calculation
        long totalDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate());
        long elapsedDays = ChronoUnit.DAYS.between(sprint.getStartDate(), LocalDate.now());
        elapsedDays = Math.max(0, Math.min(elapsedDays, totalDays));
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sprint.getEndDate()));

        boolean isOverdue = LocalDate.now().isAfter(sprint.getEndDate()) &&
                completed < total;

        return SprintDetailResponse.SprintMetrics.builder()
                .totalTasks(total)
                .completedTasks((int) completed)
                .inProgressTasks((int) inProgress)
                .todoTasks((int) todo)
                .blockedTasks((int) blocked)
                .totalStoryPoints(totalStoryPoints)
                .completedStoryPoints(completedStoryPoints)
                .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                .velocityPercentage(Math.round(velocityPercentage * 100.0) / 100.0)
                .totalDays((int) totalDays)
                .elapsedDays((int) elapsedDays)
                .remainingDays((int) remainingDays)
                .isOverdue(isOverdue)
                .build();
    }

    default SprintDetailResponse.SprintMetrics createEmptyMetrics(Sprint sprint) {
        long totalDays = sprint.getStartDate() != null && sprint.getEndDate() != null ?
                ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) : 0;
        long remainingDays = sprint.getEndDate() != null ?
                Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sprint.getEndDate())) : 0;

        return SprintDetailResponse.SprintMetrics.builder()
                .totalTasks(0)
                .completedTasks(0)
                .inProgressTasks(0)
                .todoTasks(0)
                .blockedTasks(0)
                .totalStoryPoints(0)
                .completedStoryPoints(0)
                .completionPercentage(0.0)
                .velocityPercentage(0.0)
                .totalDays((int) totalDays)
                .elapsedDays(0)
                .remainingDays((int) remainingDays)
                .isOverdue(false)
                .build();
    }
}
