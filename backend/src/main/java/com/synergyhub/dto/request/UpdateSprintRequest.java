package com.synergyhub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSprintRequest {

    @Size(max = 100, message = "Sprint name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Sprint goal must not exceed 500 characters")
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;
}