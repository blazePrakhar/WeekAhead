package com.weekahead.allocation.algorithm;

import java.util.List;

public record AllocationResult(
        Integer discretionaryMinutes,
        Integer totalRecommendedMinutes,
        List<AllocationResultItem> allocations
) {
}