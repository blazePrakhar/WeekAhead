package com.weekahead.allocation.service;

import com.weekahead.allocation.algorithm.AllocationResult;
import com.weekahead.week.entity.Week;

public record AllocationSnapshot(
        Week week,
        AllocationResult result
) {
}