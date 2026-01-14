package com.synergyhub.dto.request;

import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

import com.synergyhub.domain.enums.ProjectRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    @NoHtml(message = "Project name cannot contain HTML")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @NoHtml(allowFormatting = true, message = "Description cannot contain HTML tags")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    // Optional: Add initial members with their roles
    private Set<MemberWithRole> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWithRole {
        @NotNull(message = "User ID is required")
        private Long userId;

            @NotNull(message = "Role is required")
            private ProjectRole role;
    }
}
