package com.weekahead.analytics.dto;

import java.time.LocalDate;

public record WeeklyTrendResponse(
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer availableMinutes,
        Integer fixedCommitmentMinutes,
        Integer recommendedMinutes,
        Integer plannedMinutes,
        Integer actualMinutes,
        Integer deficitMinutes,
        Integer overflowMinutes
) {
}