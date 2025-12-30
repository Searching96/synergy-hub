package com.synergyhub.dto.request;

import com.synergyhub.domain.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Role is required")
    private ProjectRole role;
}
