package com.weekahead.analytics.dto;

public record ConsistencyResponse(
        Integer weeksAnalyzed,
        Integer weeksWithActivity,
        Integer totalActualMinutes,
        Integer totalRecommendedMinutes,
        Double averageUtilization,
        Integer consistentWeeks
) {
}