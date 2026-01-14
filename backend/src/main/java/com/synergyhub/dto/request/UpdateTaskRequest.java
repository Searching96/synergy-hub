package com.synergyhub.dto.request;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;
import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    @Size(max = 200, message = "Task title must not exceed 200 characters")
    @NoHtml(message = "Task title cannot contain HTML")
    private String title;

    @Size(max = 2000, message = "Task description must not exceed 2000 characters")
    @NoHtml(allowFormatting = true, message = "Task description cannot contain HTML tags")
    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private TaskType type;

    private Long assigneeId;

    private Long sprintId;

    @Min(value = 1, message = "Story points must be at least 1")
    @Max(value = 100, message = "Story points must not exceed 100")
    private Integer storyPoints;

    private LocalDateTime dueDate;
    
    // Position for drag-and-drop ordering (ignored by backend for now)
    private Integer position;
}