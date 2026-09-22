package com.weekahead.rebalancing.model;

public record RebalancingSuggestion(
        Long sourceLifeAreaId,
        Long destinationLifeAreaId,
        int transferableMinutes,
        String explanation
) {
}
