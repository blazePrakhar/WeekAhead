package com.weekahead.timetracking.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TimeLogResponse(
        Long id,
        Long lifeAreaId,
        String lifeAreaName,
        LocalDate logDate,
        Integer durationMinutes,
        String note,
        String source,
        LocalDateTime createdAt
) {
}