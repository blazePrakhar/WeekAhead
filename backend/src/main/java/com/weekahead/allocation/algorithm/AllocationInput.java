package com.weekahead.allocation.algorithm;

import java.util.List;

public record AllocationInput(
        Integer availableMinutes,
        Integer fixedCommitmentMinutes,
        List<LifeAreaAllocationInput> lifeAreas
) {
}