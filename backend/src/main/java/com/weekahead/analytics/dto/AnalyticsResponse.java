package com.weekahead.analytics.dto;

import java.util.List;

public record AnalyticsResponse(
        Integer weeksAnalyzed,
        List<WeeklyTrendResponse> weeklyTrends,
        ConsistencyResponse consistency,
        List<HistoricalAllocationResponse> historicalAllocation,
        List<LifeAreaComparisonResponse> lifeAreaComparisons
) {
}