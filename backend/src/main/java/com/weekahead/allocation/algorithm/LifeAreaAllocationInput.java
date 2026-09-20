package com.weekahead.allocation.algorithm;

public record LifeAreaAllocationInput(
        Long lifeAreaId,
        String name,
        Integer weight,
        Integer minMinutes,
        Integer maxMinutes
) {
}