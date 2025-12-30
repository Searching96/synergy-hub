package com.synergyhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityLogResponse {
    private Long id;
    private String action;      // e.g., "TASK_MOVED"
    private String details;     // e.g., "Task 'Login Fix' moved: To Do -> In Progress"
    private String ipAddress;
    private LocalDateTime timestamp;

    // Actor info (Who did it?)
    private Integer actorId;
    private String actorName;
    private String actorEmail;
    
    // Context (Which project?)
    private Integer projectId;
}