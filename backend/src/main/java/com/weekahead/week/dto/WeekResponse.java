package com.weekahead.week.dto;

import java.time.LocalDate;

public record WeekResponse(
        Long id,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer availableMinutes,
        Integer fixedCommitmentMinutes
) {
}