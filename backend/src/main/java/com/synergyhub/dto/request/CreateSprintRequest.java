package com.synergyhub.dto.request;

import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSprintRequest {

    @NotBlank(message = "Sprint name is required")
    @Size(max = 100, message = "Sprint name must not exceed 100 characters")
    @NoHtml(message = "Sprint name cannot contain HTML")
    private String name;

    @Size(max = 500, message = "Sprint goal must not exceed 500 characters")
    @NoHtml(allowFormatting = true, message = "Sprint goal cannot contain HTML tags")
    private String goal;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;
}
