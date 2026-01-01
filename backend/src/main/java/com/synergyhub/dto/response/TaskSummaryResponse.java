package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TaskSummaryResponse {
    private Integer id;
    private String title;
    private TaskStatus status;
    private TaskPriority priority;
    private TaskType type;
    
    // ✅ FIX: Added missing field used by SprintMapper
    private Integer storyPoints;
    private boolean archived;

    private Integer assigneeId;
    private String assigneeName;
    
    private Integer reporterId;
    private String reporterName;
    
    private boolean isOverdue;
    private boolean isSubtask;
    
    private int subtaskCount;
    private double completionPercentage;
    
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
}