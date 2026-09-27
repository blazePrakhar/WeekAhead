package com.weekahead.lifearea.dto;

import java.time.Instant;

public record LifeAreaResponse(
        Long id,
        String name,
        String description,
        Integer weight,
        Integer minMinutes,
        Integer maxMinutes,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}