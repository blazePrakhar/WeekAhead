package com.weekahead.analytics.dto;

public record LifeAreaComparisonResponse(
        Long lifeAreaId,
        String lifeAreaName,
        Integer totalRecommendedMinutes,
        Integer totalPlannedMinutes,
        Integer totalActualMinutes,
        Double averageUtilization,
        Integer totalDeficitMinutes,
        Integer totalOverflowMinutes
) {
}