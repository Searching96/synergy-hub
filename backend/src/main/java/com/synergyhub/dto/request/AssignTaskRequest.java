package com.synergyhub.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTaskRequest {

    private Integer assigneeId;  // null to unassign
}
