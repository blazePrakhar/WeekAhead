package com.weekahead.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

import com.weekahead.rebalancing.model.RebalancingSuggestion;

public record WeeklyDashboardResponse(
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer availableMinutes,
        Integer fixedCommitmentMinutes,
        Integer discretionaryMinutes,
        Integer totalRecommendedMinutes,
        Integer totalPlannedMinutes,
        Integer totalActualMinutes,
        Integer totalDeficitMinutes,
        Integer totalOverflowMinutes,
        List<DashboardLifeAreaResponse> lifeAreas,
        List<RebalancingSuggestion> rebalancingSuggestions
) {
}