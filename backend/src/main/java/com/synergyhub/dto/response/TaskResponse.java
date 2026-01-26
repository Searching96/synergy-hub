package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private TaskType type;
    
    // Project context
    private Long projectId;
    private String projectName;
    
    // Sprint context (optional)
    private Long sprintId;
    private String sprintName;
    
    // Hierarchy (for subtasks)
    private Long parentTaskId;
    private String parentTaskTitle;
    private boolean isSubtask;
    
    // Hierarchy (for epics)
    private Long epicId;
    private String epicTitle;
    
    // People
    private UserSummary reporter;
    private UserSummary assignee;
    
    // Details
    private Integer storyPoints;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean archived;
    
    // ✅ Subtasks (nested)
    private List<TaskSummaryResponse> subtasks; // Use summary to avoid deep nesting
    
    // ✅ Computed fields
    private double completionPercentage;
    private boolean isOverdue;
    private int subtaskCount; // ✅ Total number of subtasks
    private int completedSubtaskCount; // ✅ How many are done
    
    // ✅ Watching & Linking
    private boolean watching;
    private int watchersCount;
    private List<TaskSummaryResponse> linkedTasks;
    
    @Data
    @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
    }
}