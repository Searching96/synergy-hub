package com.synergyhub.dto.response;

import com.synergyhub.domain.enums.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private Long userId;
    private String name;
    private String email;
    private ProjectRole role;
    private String joinedAt;
}
