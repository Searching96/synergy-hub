package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Integer id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;

    private TaskType type;

    private Integer projectId;
    private String projectName;

    private Integer sprintId;
    private String sprintName;

    private UserResponse assignee;
    private UserResponse reporter;

    private Integer parentTaskId;
    private String parentTaskTitle;
    private Boolean isSubtask;

    private Integer storyPoints;
    private LocalDateTime dueDate;
    private Boolean isOverdue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
