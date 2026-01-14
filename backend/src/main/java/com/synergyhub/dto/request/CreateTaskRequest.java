package com.synergyhub.dto.request;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskType;
import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    @NoHtml(message = "Task title cannot contain HTML")
    private String title;

    @Size(max = 2000, message = "Task description must not exceed 2000 characters")
    @NoHtml(allowFormatting = true, message = "Task description cannot contain HTML tags")
    private String description;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long sprintId;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;
    
    // Default to TO_DO if not provided
    private com.synergyhub.domain.enums.TaskStatus status;

    private TaskType type;

    private Long assigneeId;
    
    private Long reporterId;

    private Long parentTaskId;

    @Min(value = 1, message = "Story points must be at least 1")
    @Max(value = 100, message = "Story points must not exceed 100")
    private Integer storyPoints;
    
    private String estimatedHours;

    private java.time.LocalDate dueDate;
    
    private java.time.LocalDate startDate;
    
    private java.util.List<String> labels;
}
