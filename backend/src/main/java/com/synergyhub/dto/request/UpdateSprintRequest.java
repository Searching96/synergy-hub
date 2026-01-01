package com.synergyhub.dto.request;

import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSprintRequest {

    @Size(max = 100, message = "Sprint name must not exceed 100 characters")
    @NoHtml(message = "Sprint name cannot contain HTML")
    private String name;

    @Size(max = 500, message = "Sprint goal must not exceed 500 characters")
    @NoHtml(allowFormatting = true, message = "Sprint goal cannot contain HTML tags")
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;
}