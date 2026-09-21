package com.weekahead.timetracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTimeLogRequest(

        @NotNull
        Long lifeAreaId,

        @NotNull
        LocalDate logDate,

        @NotNull
        @Positive
        Integer durationMinutes,

        @Size(max = 500)
        String note,

        @NotBlank
        @Size(max = 50)
        String source
) {
}