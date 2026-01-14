package com.synergyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineTaskResponse {
    private Long id;
    private String title;
    private TaskStatus status;
    private TaskType type;
    private String priority;
    private Integer storyPoints;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;
    
    private Long sprintId;
    private String sprintName;
    private Long assigneeId;
    private String assigneeName;
}
