package com.weekahead.week.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WeekRequest(

        @NotNull(message = "Week start date is required")
        LocalDate weekStartDate,

        @NotNull(message = "Available minutes are required")
        @Min(value = 0, message = "Available minutes must be greater than or equal to 0")
        Integer availableMinutes,

        @NotNull(message = "Fixed commitment minutes are required")
        @Min(value = 0, message = "Fixed commitment minutes must be greater than or equal to 0")
        Integer fixedCommitmentMinutes
) {
}