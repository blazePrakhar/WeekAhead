package com.weekahead.analytics.service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.analytics.dto.AnalyticsResponse;
import com.weekahead.analytics.dto.ConsistencyResponse;
import com.weekahead.analytics.dto.LifeAreaComparisonResponse;
import com.weekahead.analytics.dto.WeeklyTrendResponse;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.timetracking.entity.TimeLog;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    private AnalyticsService analyticsService;

    private User user;

    private LifeArea fitness;

    private LifeArea study;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                currentUserService,
                weekRepository,
                weeklyAllocationRepository,
                timeLogRepository
        );

        user = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        setId(user, 1L);

        fitness = new LifeArea(
                user,
                "Fitness",
                "Physical activity",
                1,
                60,
                300
        );

        study = new LifeArea(
                user,
                "Study",
                "Learning",
                1,
                60,
                300
        );

        setId(fitness, 1L);
        setId(study, 2L);
    }

    @Test
    void shouldRejectNonPositiveWeeks() {
        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getAnalytics(0)
        );
    }

    @Test
    void shouldReturnEmptyAnalyticsWhenNoCompletedWeeksExist() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        when(
                weekRepository
                        .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                                user.getId(),
                                LocalDate.now(),
                                org.springframework.data.domain.PageRequest.of(0, 12)
                        )
        ).thenReturn(List.of());

        AnalyticsResponse response = analyticsService.getAnalytics();

        assertNotNull(response);
        assertEquals(0, response.weeksAnalyzed());
        assertEquals(0, response.weeklyTrends().size());
        assertEquals(0, response.consistency().weeksAnalyzed());
        assertEquals(0, response.historicalAllocation().size());
        assertEquals(0, response.lifeAreaComparisons().size());
    }

    @Test
    void shouldCalculateWeeklyTrendUsingTimeLogsAsActualTime() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        Week week = new Week(
                user,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13),
                1000,
                200,
                "COMPLETED"
        );

        setId(week, 1L);

        when(
                weekRepository
                        .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                                user.getId(),
                                LocalDate.now(),
                                org.springframework.data.domain.PageRequest.of(0, 12)
                        )
        ).thenReturn(List.of(week));

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                fitness,
                300,
                280,
                999,
                "v1",
                null
        );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdInOrderByWeekIdAscLifeAreaIdAsc(
                                List.of(week.getId())
                        )
        ).thenReturn(List.of(allocation));

        TimeLog timeLog = new TimeLog(
                user,
                fitness,
                LocalDate.of(2026, 9, 10),
                240,
                "Workout",
                "MANUAL"
        );

        when(
                timeLogRepository
                        .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                                user.getId(),
                                week.getWeekStartDate(),
                                week.getWeekEndDate()
                        )
        ).thenReturn(List.of(timeLog));

        AnalyticsResponse response = analyticsService.getAnalytics();

        WeeklyTrendResponse trend =
                response.weeklyTrends().get(0);

        assertEquals(300, trend.recommendedMinutes());
        assertEquals(280, trend.plannedMinutes());

        // Actual time must come from TimeLog,
        // not WeeklyAllocation.actualMinutes.
        assertEquals(240, trend.actualMinutes());

        assertEquals(60, trend.deficitMinutes());
        assertEquals(0, trend.overflowMinutes());
    }

    @Test
    void shouldCalculateOverflowWhenActualExceedsRecommendation() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        Week week = new Week(
                user,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13),
                1000,
                200,
                "COMPLETED"
        );

        setId(week, 1L);

        when(
                weekRepository
                        .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                                user.getId(),
                                LocalDate.now(),
                                org.springframework.data.domain.PageRequest.of(0, 12)
                        )
        ).thenReturn(List.of(week));

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                fitness,
                300,
                300,
                0,
                "v1",
                null
        );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdInOrderByWeekIdAscLifeAreaIdAsc(
                                List.of(week.getId())
                        )
        ).thenReturn(List.of(allocation));

        TimeLog timeLog = new TimeLog(
                user,
                fitness,
                LocalDate.of(2026, 9, 10),
                360,
                "Workout",
                "MANUAL"
        );

        when(
                timeLogRepository
                        .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                                user.getId(),
                                week.getWeekStartDate(),
                                week.getWeekEndDate()
                        )
        ).thenReturn(List.of(timeLog));

        AnalyticsResponse response = analyticsService.getAnalytics();

        WeeklyTrendResponse trend =
                response.weeklyTrends().get(0);

        assertEquals(360, trend.actualMinutes());
        assertEquals(0, trend.deficitMinutes());
        assertEquals(60, trend.overflowMinutes());
    }

    @Test
    void shouldCalculateConsistencyAtEightyPercentThreshold() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        Week week1 = new Week(
                user,
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 6),
                1000,
                200,
                "COMPLETED"
        );

        Week week2 = new Week(
                user,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13),
                1000,
                200,
                "COMPLETED"
        );

        setId(week1, 1L);
        setId(week2, 2L);

        when(
                weekRepository
                        .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                                user.getId(),
                                LocalDate.now(),
                                org.springframework.data.domain.PageRequest.of(0, 12)
                        )
        ).thenReturn(List.of(week2, week1));

        WeeklyAllocation allocation1 =
                new WeeklyAllocation(
                        week1,
                        fitness,
                        300,
                        300,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation allocation2 =
                new WeeklyAllocation(
                        week2,
                        fitness,
                        300,
                        300,
                        0,
                        "v1",
                        null
                );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdInOrderByWeekIdAscLifeAreaIdAsc(
                                List.of(week1.getId(), week2.getId())
                        )
        ).thenReturn(List.of(allocation1, allocation2));

        TimeLog week1Log = new TimeLog(
                user,
                fitness,
                LocalDate.of(2026, 9, 3),
                240,
                "Workout",
                "MANUAL"
        );

        TimeLog week2Log = new TimeLog(
                user,
                fitness,
                LocalDate.of(2026, 9, 10),
                180,
                "Workout",
                "MANUAL"
        );

        when(
                timeLogRepository
                        .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                                user.getId(),
                                week1.getWeekStartDate(),
                                week2.getWeekEndDate()
                        )
        ).thenReturn(List.of(week2Log, week1Log));

        AnalyticsResponse response =
                analyticsService.getAnalytics();

        ConsistencyResponse consistency =
                response.consistency();

        assertEquals(2, consistency.weeksAnalyzed());
        assertEquals(2, consistency.weeksWithActivity());
        assertEquals(420, consistency.totalActualMinutes());
        assertEquals(600, consistency.totalRecommendedMinutes());

        // Week 1 = 240 / 300 = 80% -> consistent.
        // Week 2 = 180 / 300 = 60% -> not consistent.
        assertEquals(1, consistency.consistentWeeks());
    }

    @Test
    void shouldBuildLifeAreaComparisonFromHistoricalWeeks() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        Week week = new Week(
                user,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13),
                1000,
                200,
                "COMPLETED"
        );

        setId(week, 1L);

        when(
                weekRepository
                        .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                                user.getId(),
                                LocalDate.now(),
                                org.springframework.data.domain.PageRequest.of(0, 12)
                        )
        ).thenReturn(List.of(week));

        WeeklyAllocation fitnessAllocation =
                new WeeklyAllocation(
                        week,
                        fitness,
                        300,
                        280,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation studyAllocation =
                new WeeklyAllocation(
                        week,
                        study,
                        200,
                        180,
                        0,
                        "v1",
                        null
                );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdInOrderByWeekIdAscLifeAreaIdAsc(
                                List.of(week.getId())
                        )
        ).thenReturn(
                List.of(
                        fitnessAllocation,
                        studyAllocation
                )
        );

        TimeLog fitnessLog = new TimeLog(
                user,
                fitness,
                LocalDate.of(2026, 9, 10),
                240,
                "Workout",
                "MANUAL"
        );

        TimeLog studyLog = new TimeLog(
                user,
                study,
                LocalDate.of(2026, 9, 11),
                100,
                "Study",
                "MANUAL"
        );

        when(
                timeLogRepository
                        .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                                user.getId(),
                                week.getWeekStartDate(),
                                week.getWeekEndDate()
                        )
        ).thenReturn(
                List.of(
                        studyLog,
                        fitnessLog
                )
        );

        AnalyticsResponse response =
                analyticsService.getAnalytics();

        assertEquals(2, response.lifeAreaComparisons().size());

        LifeAreaComparisonResponse fitnessComparison =
                response.lifeAreaComparisons()
                        .stream()
                        .filter(
                                item -> item.lifeAreaId()
                                        .equals(fitness.getId())
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                300,
                fitnessComparison.totalRecommendedMinutes()
        );

        assertEquals(
                280,
                fitnessComparison.totalPlannedMinutes()
        );

        assertEquals(
                240,
                fitnessComparison.totalActualMinutes()
        );

        assertEquals(
                60,
                fitnessComparison.totalDeficitMinutes()
        );

        assertEquals(
                0,
                fitnessComparison.totalOverflowMinutes()
        );
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Could not set test entity ID",
                    e
            );
        }
    }
}