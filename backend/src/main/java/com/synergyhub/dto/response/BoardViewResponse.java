package com.synergyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardViewResponse {
    
    // The active context (usually 1, but designed as a list for future parallel sprints)
    private List<SprintDetailResponse> activeSprints;
    
    // The backlog column
    private List<TaskResponse> backlogTasks;
}