package com.weekahead.ai.dto;

public record AILifeAreaSummary(
        Long lifeAreaId,
        String lifeAreaName,
        int recommendedMinutes,
        int plannedMinutes,
        int actualMinutes,
        double utilization,
        String neglectLevel,
        int consecutiveUnderTargetWeeks
) {
}