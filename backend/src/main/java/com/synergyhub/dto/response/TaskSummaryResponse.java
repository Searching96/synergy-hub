package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryResponse {

    private Integer id;
    private String title;
    private TaskStatus status;
    private TaskPriority priority;

    private Integer assigneeId;
    private String assigneeName;

    private Integer storyPoints;
    private Boolean isOverdue;
}
