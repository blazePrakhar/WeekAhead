package com.weekahead.allocation.algorithm;

public record AllocationResultItem(
        Long lifeAreaId,
        String name,
        Integer weight,
        Integer minMinutes,
        Integer maxMinutes,
        Integer recommendedMinutes
) {
}