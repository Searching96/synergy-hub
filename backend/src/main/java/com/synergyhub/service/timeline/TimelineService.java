package com.synergyhub.service.timeline;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.Sprint;
import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.TimelineSprintResponse;
import com.synergyhub.dto.response.TimelineTaskResponse;
import com.synergyhub.dto.response.TimelineViewResponse;
import com.synergyhub.exception.ProjectNotFoundException;
import com.synergyhub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {
    
    private final ProjectRepository projectRepository;
    
    @PreAuthorize("@projectSecurity.hasProjectAccess(#projectId, #currentUser)")
    @Transactional(readOnly = true)
    public TimelineViewResponse getProjectTimeline(Long projectId, User currentUser, Integer monthsAhead) {
        log.info("Getting timeline for project: {} for user: {}", projectId, currentUser.getId());
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        
        // Determine date range
        LocalDate today = LocalDate.now();
        LocalDate viewStartDate = today.minusMonths(1);
        LocalDate viewEndDate = today.plusMonths(monthsAhead != null ? monthsAhead : 6);
        
        // Get sprints in the date range
        List<TimelineSprintResponse> sprints = project.getSprints().stream()
                .filter(sprint -> sprint.getStartDate() != null && sprint.getEndDate() != null)
                .filter(sprint -> !sprint.getEndDate().isBefore(viewStartDate) && !sprint.getStartDate().isAfter(viewEndDate))
                .map(this::mapSprintToTimeline)
                .sorted(Comparator.comparing(TimelineSprintResponse::getStartDate))
                .collect(Collectors.toList());
        
        // Get tasks in the date range
        List<TimelineTaskResponse> tasks = project.getTasks().stream()
                .filter(task -> task.getDueDate() != null || task.getCreatedAt() != null)
                .filter(task -> {
                    LocalDate taskDate = task.getDueDate() != null ? task.getDueDate().toLocalDate() : task.getCreatedAt().toLocalDate();
                    return !taskDate.isBefore(viewStartDate) && !taskDate.isAfter(viewEndDate);
                })
                .map(this::mapTaskToTimeline)
                .sorted(Comparator.comparing(t -> t.getDueDate() != null ? t.getDueDate() : t.getCreatedAt()))
                .collect(Collectors.toList());
        
        return TimelineViewResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .viewStartDate(viewStartDate)
                .viewEndDate(viewEndDate)
                .sprints(sprints)
                .tasks(tasks)
                .build();
    }
    
    private TimelineSprintResponse mapSprintToTimeline(Sprint sprint) {
        long completedTasks = sprint.getTasks().stream()
                .filter(task -> "DONE".equals(task.getStatus().toString()))
                .count();
        
        int totalTasks = sprint.getTasks().size();
        double completionPercentage = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
        
        return TimelineSprintResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .status(sprint.getStatus().toString())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .totalTasks(totalTasks)
                .completedTasks((int) completedTasks)
                .completionPercentage(completionPercentage)
                .build();
    }
    
    private TimelineTaskResponse mapTaskToTimeline(Task task) {
        return TimelineTaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .type(task.getType())
                .priority(task.getPriority() != null ? task.getPriority().toString() : null)
                .storyPoints(task.getStoryPoints())
                .dueDate(task.getDueDate() != null ? task.getDueDate().toLocalDate() : null)
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toLocalDate() : null)
                .sprintId(task.getSprint() != null ? task.getSprint().getId() : null)
                .sprintName(task.getSprint() != null ? task.getSprint().getName() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .build();
    }
}
