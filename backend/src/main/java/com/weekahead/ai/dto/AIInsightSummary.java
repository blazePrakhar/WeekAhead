package com.weekahead.ai.dto;

import java.util.List;

public record AIInsightSummary(
        Long weekId,
        String weekStartDate,
        String weekEndDate,
        int availableMinutes,
        int fixedCommitmentMinutes,
        List<AILifeAreaSummary> lifeAreas,
        List<AIRebalancingSummary> rebalancingSuggestions
) {
}