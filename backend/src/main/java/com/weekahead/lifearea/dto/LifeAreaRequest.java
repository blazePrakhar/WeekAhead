package com.weekahead.lifearea.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LifeAreaRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Weight is required")
        @Min(value = 1, message = "Weight must be greater than 0")
        Integer weight,

        @NotNull(message = "Minimum minutes is required")
        @Min(value = 0, message = "Minimum minutes must be at least 0")
        Integer minMinutes,

        @NotNull(message = "Maximum minutes is required")
        @Min(value = 0, message = "Maximum minutes must be at least 0")
        Integer maxMinutes
) {}