package com.synergyhub.service.sprint;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class SprintValidator {
    public static void validateSprintDates(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // Start date validation
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Sprint start date cannot be in the past");
        }

        // End date validation
        if (endDate.isBefore(today)) {
            throw new IllegalArgumentException("Sprint end date cannot be in the past");
        }

        // Date range validation
        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("Sprint end date must be after start date");
        }

        // Optional: Sprint duration validation
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween < 7) {
            throw new IllegalArgumentException("Sprint duration must be at least 7 days");
        }
        if (daysBetween > 32) {
            throw new IllegalArgumentException("Sprint duration cannot exceed 32 days");
        }
    }
}
