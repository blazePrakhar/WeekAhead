package com.weekahead.analytics.dto;

import java.util.List;

public record HistoricalAllocationResponse(
        Long lifeAreaId,
        String lifeAreaName,
        List<Integer> weeklyRecommendedMinutes
) {
}