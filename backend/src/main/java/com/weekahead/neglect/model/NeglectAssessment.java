package com.weekahead.neglect.model;

public record NeglectAssessment(
        Long lifeAreaId,
        double utilization,
        int consecutiveUnderTargetWeeks,
        NeglectLevel level
) {
}