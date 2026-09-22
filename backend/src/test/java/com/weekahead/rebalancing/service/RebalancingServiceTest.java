package com.weekahead.rebalancing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.rebalancing.model.RebalancingCalculator;
import com.weekahead.rebalancing.model.RebalancingSuggestion;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@ExtendWith(MockitoExtension.class)
class RebalancingServiceTest {

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private LifeAreaRepository lifeAreaRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RebalancingCalculator rebalancingCalculator;

    private RebalancingService rebalancingService;

    private User user;
    private Week currentWeek;
    private LifeArea health;
    private LifeArea learning;

    @BeforeEach
    void setUp() {

        rebalancingService =
                new RebalancingService(
                        weekRepository,
                        weeklyAllocationRepository,
                        timeLogRepository,
                        lifeAreaRepository,
                        currentUserService,
                        rebalancingCalculator
                );

        user = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        currentWeek = new Week(
                user,
                LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(4),
                600,
                100,
                null
        );

        health = new LifeArea(
                user,
                "Health",
                null,
                1,
                60,
                300
        );

        learning = new LifeArea(
                user,
                "Learning",
                null,
                3,
                60,
                300
        );

        setId(health, 1L);
        setId(learning, 2L);
        setId(currentWeek, 1L);
    }

    @Test
    void shouldCalculateRebalancingForCurrentWeek() {

        WeeklyAllocation healthAllocation =
                new WeeklyAllocation(
                        currentWeek,
                        health,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation learningAllocation =
                new WeeklyAllocation(
                        currentWeek,
                        learning,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        List<Object[]> actualMinutes =
                List.<Object[]>of(
                        new Object[]{1L, 140},
                        new Object[]{2L, 60}
                );

        RebalancingSuggestion suggestion =
                new RebalancingSuggestion(
                        1L,
                        2L,
                        40,
                        "Transfer 40 minutes from Health to Learning."
                );

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        user.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.of(currentWeek));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(
                        currentWeek.getId()
                ))
                .thenReturn(
                        List.of(
                                healthAllocation,
                                learningAllocation
                        )
                );

        when(timeLogRepository.sumDurationByLifeArea(
                user.getId(),
                currentWeek.getWeekStartDate(),
                currentWeek.getWeekEndDate()
        ))
                .thenReturn(actualMinutes);

        when(lifeAreaRepository.findAllByUserId(
                user.getId()
        ))
                .thenReturn(
                        List.of(
                                health,
                                learning
                        )
                );

        when(rebalancingCalculator.calculate(
                anyList(),
                org.mockito.ArgumentMatchers.eq(300)
        ))
                .thenReturn(
                        List.of(suggestion)
                );

        List<RebalancingSuggestion> result =
                rebalancingService.calculate();

        assertEquals(1, result.size());
        assertEquals(suggestion, result.get(0));
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoAllocations() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        user.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.of(currentWeek));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(
                        currentWeek.getId()
                ))
                .thenReturn(List.of());

        List<RebalancingSuggestion> result =
                rebalancingService.calculate();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenCurrentWeekDoesNotExist() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        user.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> rebalancingService.calculate()
        );
    }

    @Test
    void shouldCalculateRemainingWeeklyBudgetCorrectly() {

        WeeklyAllocation healthAllocation =
                new WeeklyAllocation(
                        currentWeek,
                        health,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation learningAllocation =
                new WeeklyAllocation(
                        currentWeek,
                        learning,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        user.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.of(currentWeek));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(
                        currentWeek.getId()
                ))
                .thenReturn(
                        List.of(
                                healthAllocation,
                                learningAllocation
                        )
                );

        List<Object[]> actualMinutes =
                List.<Object[]>of(
                        new Object[]{1L, 140},
                        new Object[]{2L, 60}
                );

        when(timeLogRepository.sumDurationByLifeArea(
                user.getId(),
                currentWeek.getWeekStartDate(),
                currentWeek.getWeekEndDate()
        ))
                .thenReturn(actualMinutes);

        when(lifeAreaRepository.findAllByUserId(
                user.getId()
        ))
                .thenReturn(
                        List.of(
                                health,
                                learning
                        )
                );

        when(rebalancingCalculator.calculate(
                anyList(),
                org.mockito.ArgumentMatchers.eq(300)
        ))
                .thenReturn(
                        List.<RebalancingSuggestion>of()
                );

        List<RebalancingSuggestion> result =
                rebalancingService.calculate();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldUseHistoricalWeeklyRecommendations() {

        WeeklyAllocation healthAllocation =
                new WeeklyAllocation(
                        currentWeek,
                        health,
                        180,
                        100,
                        0,
                        "v1",
                        null
                );

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        user.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.of(currentWeek));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(
                        currentWeek.getId()
                ))
                .thenReturn(
                        List.of(healthAllocation)
                );

        List<Object[]> actualMinutes =
                List.<Object[]>of(
                        new Object[]{1L, 140}
                );

        when(timeLogRepository.sumDurationByLifeArea(
                user.getId(),
                currentWeek.getWeekStartDate(),
                currentWeek.getWeekEndDate()
        ))
                .thenReturn(actualMinutes);

        when(lifeAreaRepository.findAllByUserId(
                user.getId()
        ))
                .thenReturn(
                        List.of(health)
                );

        when(rebalancingCalculator.calculate(
                argThat(
                        inputs ->
                                inputs.size() == 1
                                        && inputs.get(0)
                                                .recommendedMinutes() == 180
                ),
                anyInt()
        ))
                .thenReturn(
                        List.<RebalancingSuggestion>of()
                );

        List<RebalancingSuggestion> result =
                rebalancingService.calculate();

        assertTrue(result.isEmpty());
    }

    private void setId(
            LifeArea lifeArea,
            Long id
    ) {
        try {
            var field =
                    LifeArea.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(lifeArea, id);

        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to set LifeArea test ID",
                    exception
            );
        }
    }

    private void setId(
            Week week,
            Long id
    ) {
        try {
            var field =
                    Week.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(week, id);

        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to set Week test ID",
                    exception
            );
        }
    }
}