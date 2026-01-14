package com.synergyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineViewResponse {
    private Long projectId;
    private String projectName;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate viewStartDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate viewEndDate;
    
    private List<TimelineSprintResponse> sprints;
    private List<TimelineTaskResponse> tasks;
}
