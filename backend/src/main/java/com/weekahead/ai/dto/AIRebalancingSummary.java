package com.weekahead.ai.dto;

public record AIRebalancingSummary(
        Long sourceLifeAreaId,
        Long destinationLifeAreaId,
        int transferableMinutes,
        String explanation
) {
}