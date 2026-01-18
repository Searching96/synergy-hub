package com.synergyhub.dto.request;

import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.synergyhub.domain.enums.ProjectStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @Size(max = 100, message = "Project name must not exceed 100 characters")
    @NoHtml(message = "Project name cannot contain HTML")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @NoHtml(allowFormatting = true, message = "Description cannot contain HTML tags")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status; // ACTIVE, COMPLETED, ARCHIVED

    private Long teamId;
}
