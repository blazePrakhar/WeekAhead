package com.weekahead.rebalancing.model;

public record RebalancingInput(
        Long lifeAreaId,
        String lifeAreaName,
        int recommendedMinutes,
        int actualMinutes,
        int minimumMinutes,
        double priorityWeight
) {
}