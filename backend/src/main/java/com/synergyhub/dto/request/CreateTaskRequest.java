package com.synergyhub.dto.request;

import com.synergyhub.domain.enums.TaskPriority;
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
    private String title;

    @Size(max = 2000, message = "Task description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Project ID is required")
    private Integer projectId;

    private Integer sprintId;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    private Integer assigneeId;

    @Min(value = 1, message = "Story points must be at least 1")
    @Max(value = 100, message = "Story points must not exceed 100")
    private Integer storyPoints;

    private LocalDateTime dueDate;
}
