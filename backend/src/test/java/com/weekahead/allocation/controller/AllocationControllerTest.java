package com.weekahead.allocation.controller;

import com.weekahead.allocation.algorithm.AllocationResult;
import com.weekahead.allocation.algorithm.AllocationResultItem;
import com.weekahead.allocation.service.AllocationService;
import com.weekahead.allocation.service.AllocationSnapshot;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.exception.GlobalExceptionHandler;
import com.weekahead.week.entity.Week;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AllocationControllerTest {

    @Mock
    private AllocationService allocationService;

    private MockMvc mockMvc;

    private Week week;

    @BeforeEach
    void setUp() {

        AllocationController controller
                = new AllocationController(allocationService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        User user = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        week = new Week(
                user,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                600,
                100,
                null
        );
    }

    private AllocationSnapshot createSnapshot() {

        AllocationResult result
                = new AllocationResult(
                        500,
                        500,
                        List.of(
                                new AllocationResultItem(
                                        1L,
                                        "Career",
                                        2,
                                        60,
                                        300,
                                        300
                                ),
                                new AllocationResultItem(
                                        2L,
                                        "Health",
                                        1,
                                        30,
                                        200,
                                        200
                                )
                        )
                );

        return new AllocationSnapshot(week, result);
    }

    @Test
    void shouldGenerateRecommendation() throws Exception {

        when(allocationService.generateRecommendation(1L))
                .thenReturn(createSnapshot());

        mockMvc.perform(
                post("/api/weeks/1/recommendation/generate")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekId")
                        .value(week.getId()))
                // LocalDate is serialized as [year, month, day]
                .andExpect(jsonPath("$.weekStartDate[0]")
                        .value(2026))
                .andExpect(jsonPath("$.weekStartDate[1]")
                        .value(9))
                .andExpect(jsonPath("$.weekStartDate[2]")
                        .value(21))
                .andExpect(jsonPath("$.weekEndDate[0]")
                        .value(2026))
                .andExpect(jsonPath("$.weekEndDate[1]")
                        .value(9))
                .andExpect(jsonPath("$.weekEndDate[2]")
                        .value(27))
                .andExpect(jsonPath("$.availableMinutes")
                        .value(600))
                .andExpect(jsonPath("$.fixedCommitmentMinutes")
                        .value(100))
                .andExpect(jsonPath("$.discretionaryMinutes")
                        .value(500))
                .andExpect(jsonPath("$.totalRecommendedMinutes")
                        .value(500))
                .andExpect(jsonPath("$.allocations")
                        .isArray())
                .andExpect(jsonPath("$.allocations.length()")
                        .value(2))
                .andExpect(jsonPath("$.allocations[0].lifeAreaId")
                        .value(1))
                .andExpect(jsonPath("$.allocations[0].lifeAreaName")
                        .value("Career"))
                .andExpect(jsonPath("$.allocations[0].weight")
                        .value(2))
                .andExpect(jsonPath("$.allocations[0].recommendedMinutes")
                        .value(300))
                .andExpect(jsonPath("$.allocations[0].minMinutes")
                        .value(60))
                .andExpect(jsonPath("$.allocations[0].maxMinutes")
                        .value(300))
                .andExpect(jsonPath("$.allocations[1].lifeAreaId")
                        .value(2))
                .andExpect(jsonPath("$.allocations[1].lifeAreaName")
                        .value("Health"))
                .andExpect(jsonPath("$.allocations[1].weight")
                        .value(1))
                .andExpect(jsonPath("$.allocations[1].recommendedMinutes")
                        .value(200))
                .andExpect(jsonPath("$.allocations[1].minMinutes")
                        .value(30))
                .andExpect(jsonPath("$.allocations[1].maxMinutes")
                        .value(200));

        verify(allocationService)
                .generateRecommendation(1L);
    }

    @Test
    void shouldRetrieveAllocations() throws Exception {

        when(allocationService.getAllocations(1L))
                .thenReturn(createSnapshot());

        mockMvc.perform(
                get("/api/weeks/1/allocations")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekId")
                        .value(week.getId()))
                .andExpect(jsonPath("$.weekStartDate[0]")
                        .value(2026))
                .andExpect(jsonPath("$.weekStartDate[1]")
                        .value(9))
                .andExpect(jsonPath("$.weekStartDate[2]")
                        .value(21))
                .andExpect(jsonPath("$.weekEndDate[0]")
                        .value(2026))
                .andExpect(jsonPath("$.weekEndDate[1]")
                        .value(9))
                .andExpect(jsonPath("$.weekEndDate[2]")
                        .value(27))
                .andExpect(jsonPath("$.availableMinutes")
                        .value(600))
                .andExpect(jsonPath("$.fixedCommitmentMinutes")
                        .value(100))
                .andExpect(jsonPath("$.discretionaryMinutes")
                        .value(500))
                .andExpect(jsonPath("$.totalRecommendedMinutes")
                        .value(500))
                .andExpect(jsonPath("$.allocations.length()")
                        .value(2))
                .andExpect(jsonPath("$.allocations[0].lifeAreaName")
                        .value("Career"))
                .andExpect(jsonPath("$.allocations[0].recommendedMinutes")
                        .value(300))
                .andExpect(jsonPath("$.allocations[1].lifeAreaName")
                        .value("Health"))
                .andExpect(jsonPath("$.allocations[1].recommendedMinutes")
                        .value(200));

        verify(allocationService)
                .getAllocations(1L);
    }

    @Test
    void shouldPropagateServiceErrorWhenWeekIsMissing()
            throws Exception {

        when(allocationService.generateRecommendation(999L))
                .thenThrow(
                        new IllegalArgumentException("Week not found")
                );

        mockMvc.perform(
                post("/api/weeks/999/recommendation/generate")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Week not found"));

        verify(allocationService)
                .generateRecommendation(999L);
    }

    @Test
    void shouldPropagateServiceErrorWhenAllocationsDoNotExist()
            throws Exception {

        when(allocationService.getAllocations(999L))
                .thenThrow(
                        new IllegalArgumentException(
                                "Allocation recommendation not found"
                        )
                );

        mockMvc.perform(
                get("/api/weeks/999/allocations")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Allocation recommendation not found"));

        verify(allocationService)
                .getAllocations(999L);
    }
}
