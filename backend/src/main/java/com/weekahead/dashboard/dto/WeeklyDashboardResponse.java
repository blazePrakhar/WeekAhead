package com.weekahead.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyDashboardResponse(
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer availableMinutes,
        Integer fixedCommitmentMinutes,
        Integer discretionaryMinutes,
        Integer totalRecommendedMinutes,
        Integer totalActualMinutes,
        Integer totalDeficitMinutes,
        Integer totalOverflowMinutes,
        List<DashboardLifeAreaResponse> lifeAreas
) {
}