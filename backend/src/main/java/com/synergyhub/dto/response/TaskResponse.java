package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskResponse {
    private Integer id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private TaskType type;
    
    // Project context
    private Integer projectId;
    private String projectName;
    
    // Sprint context (optional)
    private Integer sprintId;
    private String sprintName;
    
    // Hierarchy (for subtasks)
    private Integer parentTaskId;
    private String parentTaskTitle;
    private boolean isSubtask;
    
    // People
    private UserSummary reporter;
    private UserSummary assignee;
    
    // Details
    private Integer storyPoints;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ✅ Subtasks (nested)
    private List<TaskSummaryResponse> subtasks; // Use summary to avoid deep nesting
    
    // ✅ Computed fields
    private double completionPercentage;
    private boolean isOverdue;
    private int subtaskCount; // ✅ Total number of subtasks
    private int completedSubtaskCount; // ✅ How many are done
    
    @Data
    @Builder
    public static class UserSummary {
        private Integer id;
        private String name;
        private String email;
    }
}