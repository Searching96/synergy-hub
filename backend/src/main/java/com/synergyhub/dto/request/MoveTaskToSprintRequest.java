package com.synergyhub.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveTaskToSprintRequest {

    private Long sprintId;
}
