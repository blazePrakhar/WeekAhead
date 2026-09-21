package com.weekahead.allocation.dto;

import java.time.LocalDate;
import java.util.List;

public record AllocationResponse(
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer availableMinutes,
        Integer fixedCommitmentMinutes,
        Integer discretionaryMinutes,
        Integer totalRecommendedMinutes,
        List<AllocationItem> allocations
        ) {

    public record AllocationItem(
            Long lifeAreaId,
            String lifeAreaName,
            Integer weight,
            Integer recommendedMinutes,
            Integer minMinutes,
            Integer maxMinutes
            ) {

    }
}
