package com.weekahead.allocation.service;

import com.weekahead.allocation.algorithm.AllocationEngine;
import com.weekahead.allocation.algorithm.AllocationResult;
import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private LifeAreaRepository lifeAreaRepository;

    @Mock
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @Mock
    private AllocationEngine allocationEngine;

    private AllocationService allocationService;

    private User user;
    private Week week;
    private LifeArea lifeArea;

    @BeforeEach
    void setUp() {
        allocationService = new AllocationService(
                currentUserService,
                weekRepository,
                lifeAreaRepository,
                weeklyAllocationRepository,
                allocationEngine
        );

        user = new User(
                "test@example.com",
                "hashed-password",
                com.weekahead.auth.entity.Role.USER,
                com.weekahead.auth.entity.UserStatus.ACTIVE
        );

        week = new Week(
                user,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                600,
                100,
                null
        );

        lifeArea = mock(LifeArea.class);
    }

    private void stubActiveLifeArea() {
        when(lifeArea.getId()).thenReturn(1L);
        when(lifeArea.getName()).thenReturn("Career");
        when(lifeArea.getWeight()).thenReturn(1);
        when(lifeArea.getMinMinutes()).thenReturn(60);
        when(lifeArea.getMaxMinutes()).thenReturn(300);
        when(lifeArea.getIsActive()).thenReturn(true);
    }

    @Test
    void shouldGenerateRecommendationForAuthenticatedUser() {

        stubActiveLifeArea();

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(week));

        when(lifeAreaRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of(lifeArea));

        AllocationResult result = new AllocationResult(
                500,
                500,
                List.of(
                        new com.weekahead.allocation.algorithm.AllocationResultItem(
                                lifeArea.getId(),
                                lifeArea.getName(),
                                lifeArea.getWeight(),
                                lifeArea.getMinMinutes(),
                                lifeArea.getMaxMinutes(),
                                500
                        )
                )
        );

        when(allocationEngine.calculate(any()))
                .thenReturn(result);

        when(weeklyAllocationRepository
                .findByWeekIdAndLifeAreaId(1L, lifeArea.getId()))
                .thenReturn(Optional.empty());

        AllocationSnapshot actual
                = allocationService.generateRecommendation(1L);

        assertEquals(week, actual.week());
        assertEquals(500, actual.result().totalRecommendedMinutes());
        assertEquals(1, actual.result().allocations().size());

        verify(weeklyAllocationRepository)
                .save(any(WeeklyAllocation.class));

        verify(allocationEngine)
                .calculate(any());
    }

    @Test
    void shouldIgnoreInactiveLifeAreas() {

        stubActiveLifeArea();

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(week));

        LifeArea inactiveLifeArea = mock(LifeArea.class);

        when(inactiveLifeArea.getIsActive())
                .thenReturn(false);

        when(lifeAreaRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of(lifeArea, inactiveLifeArea));

        AllocationResult result = new AllocationResult(
                500,
                500,
                List.of(
                        new com.weekahead.allocation.algorithm.AllocationResultItem(
                                lifeArea.getId(),
                                lifeArea.getName(),
                                lifeArea.getWeight(),
                                lifeArea.getMinMinutes(),
                                lifeArea.getMaxMinutes(),
                                500
                        )
                )
        );

        when(allocationEngine.calculate(any()))
                .thenReturn(result);

        when(weeklyAllocationRepository
                .findByWeekIdAndLifeAreaId(1L, lifeArea.getId()))
                .thenReturn(Optional.empty());

        allocationService.generateRecommendation(1L);

        ArgumentCaptor<com.weekahead.allocation.algorithm.AllocationInput> captor
                = ArgumentCaptor.forClass(
                        com.weekahead.allocation.algorithm.AllocationInput.class
                );

        verify(allocationEngine)
                .calculate(captor.capture());

        assertEquals(1, captor.getValue().lifeAreas().size());

        assertEquals(
                lifeArea.getId(),
                captor.getValue().lifeAreas().get(0).lifeAreaId()
        );
    }

    @Test
    void shouldRejectMissingWeek() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception
                = assertThrows(
                        IllegalArgumentException.class,
                        () -> allocationService.generateRecommendation(1L)
                );

        assertEquals("Week not found", exception.getMessage());

        verifyNoInteractions(lifeAreaRepository);
        verifyNoInteractions(allocationEngine);
        verifyNoInteractions(weeklyAllocationRepository);
    }

    @Test
    void shouldRejectWhenNoActiveLifeAreasExist() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(week));

        when(lifeAreaRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of());

        IllegalArgumentException exception
                = assertThrows(
                        IllegalArgumentException.class,
                        () -> allocationService.generateRecommendation(1L)
                );

        assertEquals(
                "No active Life Areas found",
                exception.getMessage()
        );

        verifyNoInteractions(allocationEngine);
        verifyNoInteractions(weeklyAllocationRepository);
    }

    @Test
    void shouldPreservePlannedAndActualMinutesDuringRegeneration() {

        stubActiveLifeArea();

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(week));

        when(lifeAreaRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of(lifeArea));

        AllocationResult result = new AllocationResult(
                500,
                500,
                List.of(
                        new com.weekahead.allocation.algorithm.AllocationResultItem(
                                lifeArea.getId(),
                                lifeArea.getName(),
                                lifeArea.getWeight(),
                                lifeArea.getMinMinutes(),
                                lifeArea.getMaxMinutes(),
                                500
                        )
                )
        );

        when(allocationEngine.calculate(any()))
                .thenReturn(result);

        WeeklyAllocation existingAllocation
                = new WeeklyAllocation(
                        week,
                        lifeArea,
                        450,
                        400,
                        350,
                        "v1",
                        "Previous recommendation"
                );

        when(weeklyAllocationRepository
                .findByWeekIdAndLifeAreaId(1L, lifeArea.getId()))
                .thenReturn(Optional.of(existingAllocation));

        AllocationSnapshot actual
                = allocationService.generateRecommendation(1L);

        assertEquals(week, actual.week());
        assertEquals(500, actual.result().totalRecommendedMinutes());

        assertEquals(500, existingAllocation.getRecommendedMinutes());
        assertEquals(400, existingAllocation.getPlannedMinutes());
        assertEquals(350, existingAllocation.getActualMinutes());
        assertEquals("v1", existingAllocation.getAlgorithmVersion());

        verify(weeklyAllocationRepository)
                .save(existingAllocation);
    }

    @Test
    void shouldRetrieveAllocationsForOwnedWeek() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(week));

        when(lifeArea.getId()).thenReturn(1L);
        when(lifeArea.getName()).thenReturn("Career");
        when(lifeArea.getWeight()).thenReturn(1);
        when(lifeArea.getMinMinutes()).thenReturn(60);
        when(lifeArea.getMaxMinutes()).thenReturn(300);

        WeeklyAllocation allocation
                = new WeeklyAllocation(
                        week,
                        lifeArea,
                        500,
                        0,
                        0,
                        "v1",
                        "Recommendation"
                );

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(1L))
                .thenReturn(List.of(allocation));

        AllocationSnapshot result
                = allocationService.getAllocations(1L);

        assertEquals(week, result.week());
        assertEquals(
                1,
                result.result().allocations().size()
        );
        assertEquals(
                500,
                result.result().allocations().get(0).recommendedMinutes()
        );
        assertEquals(
                500,
                result.result().totalRecommendedMinutes()
        );
    }

    @Test
    void shouldRejectRetrievalForMissingOrForeignWeek() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception
                = assertThrows(
                        IllegalArgumentException.class,
                        () -> allocationService.getAllocations(1L)
                );

        assertEquals("Week not found", exception.getMessage());

        verifyNoInteractions(weeklyAllocationRepository);
    }

    @Test
    void shouldRejectRetrievalWhenRecommendationDoesNotExist() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(week));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(1L))
                .thenReturn(List.of());

        IllegalArgumentException exception
                = assertThrows(
                        IllegalArgumentException.class,
                        () -> allocationService.getAllocations(1L)
                );

        assertEquals(
                "Allocation recommendation not found",
                exception.getMessage()
        );
    }
}
