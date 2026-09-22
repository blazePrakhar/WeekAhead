package com.weekahead.neglect.service;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.neglect.model.NeglectAssessment;
import com.weekahead.neglect.model.NeglectLevel;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.service.WeekService;

@ExtendWith(MockitoExtension.class)
class NeglectServiceTest {

    @Mock
    private WeekService weekService;

    @Mock
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private LifeArea lifeArea;

    @Mock
    private LifeArea secondLifeArea;

    private NeglectService neglectService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        neglectService = new NeglectService(
                weekService,
                weeklyAllocationRepository,
                timeLogRepository,
                currentUserService
        );

        currentUser = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);
    }

    @Test
    void shouldTrackNeglectSeparatelyForMultipleLifeAreas() {

        when(lifeArea.getId()).thenReturn(1L);
        when(secondLifeArea.getId()).thenReturn(2L);

        Week weekOne = createWeek(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 6)
        );

        Week weekTwo = createWeek(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13)
        );

        WeeklyAllocation fitnessWeekOne =
                new WeeklyAllocation(
                        weekOne,
                        lifeArea,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation careerWeekOne =
                new WeeklyAllocation(
                        weekOne,
                        secondLifeArea,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation fitnessWeekTwo =
                new WeeklyAllocation(
                        weekTwo,
                        lifeArea,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        WeeklyAllocation careerWeekTwo =
                new WeeklyAllocation(
                        weekTwo,
                        secondLifeArea,
                        100,
                        100,
                        0,
                        "v1",
                        null
                );

        when(weekService.getLatestCompletedWeeks())
                .thenReturn(List.of(weekTwo, weekOne));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(weekOne.getId()))
                .thenReturn(List.of(
                        fitnessWeekOne,
                        careerWeekOne
                ));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(weekTwo.getId()))
                .thenReturn(List.of(
                        fitnessWeekTwo,
                        careerWeekTwo
                ));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                weekOne.getWeekStartDate(),
                weekOne.getWeekEndDate()
        )).thenReturn(
                List.<Object[]>of(
                        new Object[]{1L, 40L},
                        new Object[]{2L, 100L}
                )
        );

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                weekTwo.getWeekStartDate(),
                weekTwo.getWeekEndDate()
        )).thenReturn(
                List.<Object[]>of(
                        new Object[]{1L, 50L},
                        new Object[]{2L, 100L}
                )
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        assertEquals(2, result.size());

        NeglectAssessment fitness =
                result.stream()
                        .filter(a -> a.lifeAreaId().equals(1L))
                        .findFirst()
                        .orElseThrow();

        NeglectAssessment career =
                result.stream()
                        .filter(a -> a.lifeAreaId().equals(2L))
                        .findFirst()
                        .orElseThrow();

        assertEquals(0.50, fitness.utilization(), 0.0001);
        assertEquals(2, fitness.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.WARNING, fitness.level());

        assertEquals(1.0, career.utilization(), 0.0001);
        assertEquals(0, career.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, career.level());
    }

    @Test
    void shouldCalculateFortyPercentUtilization() {

        when(lifeArea.getId()).thenReturn(1L);

        Week week = createWeek(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20)
        );

        WeeklyAllocation allocation =
                createAllocation(week, 100);

        mockWeekData(
                week,
                allocation,
                new Object[]{1L, 40L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.40, assessment.utilization(), 0.0001);
        assertEquals(1, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, assessment.level());
    }

    @Test
    void shouldHandleZeroActualMinutes() {

        when(lifeArea.getId()).thenReturn(1L);

        Week week = createWeek(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20)
        );

        WeeklyAllocation allocation =
                createAllocation(week, 100);

        mockWeekData(
                week,
                allocation
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.0, assessment.utilization(), 0.0001);
        assertEquals(1, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, assessment.level());
    }

    @Test
    void shouldHandleZeroRecommendedMinutes() {

        when(lifeArea.getId()).thenReturn(1L);

        Week week = createWeek(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20)
        );

        WeeklyAllocation allocation =
                createAllocation(week, 0);

        mockWeekData(
                week,
                allocation,
                new Object[]{1L, 40L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.0, assessment.utilization(), 0.0001);
        assertEquals(1, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, assessment.level());
    }

    @Test
    void shouldReturnWarningAfterTwoConsecutiveUnderTargetWeeks() {

        when(lifeArea.getId()).thenReturn(1L);

        Week weekOne = createWeek(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 6)
        );

        Week weekTwo = createWeek(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13)
        );

        WeeklyAllocation allocationOne =
                createAllocation(weekOne, 100);

        WeeklyAllocation allocationTwo =
                createAllocation(weekTwo, 100);

        mockMultipleWeeks(
                List.of(weekTwo, weekOne),
                List.of(allocationOne, allocationTwo),
                new Object[]{1L, 40L},
                new Object[]{1L, 50L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.50, assessment.utilization(), 0.0001);
        assertEquals(2, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.WARNING, assessment.level());
    }

    @Test
    void shouldReturnHighAfterThreeConsecutiveUnderTargetWeeks() {

        when(lifeArea.getId()).thenReturn(1L);

        Week weekOne = createWeek(
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 30)
        );

        Week weekTwo = createWeek(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 6)
        );

        Week weekThree = createWeek(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13)
        );

        mockMultipleWeeks(
                List.of(weekThree, weekTwo, weekOne),
                List.of(
                        createAllocation(weekOne, 100),
                        createAllocation(weekTwo, 100),
                        createAllocation(weekThree, 100)
                ),
                new Object[]{1L, 40L},
                new Object[]{1L, 50L},
                new Object[]{1L, 60L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.60, assessment.utilization(), 0.0001);
        assertEquals(3, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.HIGH, assessment.level());
    }

    @Test
    void shouldReturnCriticalAfterFourConsecutiveUnderTargetWeeks() {

        when(lifeArea.getId()).thenReturn(1L);

        Week weekOne = createWeek(
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 23)
        );

        Week weekTwo = createWeek(
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 30)
        );

        Week weekThree = createWeek(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 6)
        );

        Week weekFour = createWeek(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13)
        );

        mockMultipleWeeks(
                List.of(weekFour, weekThree, weekTwo, weekOne),
                List.of(
                        createAllocation(weekOne, 100),
                        createAllocation(weekTwo, 100),
                        createAllocation(weekThree, 100),
                        createAllocation(weekFour, 100)
                ),
                new Object[]{1L, 40L},
                new Object[]{1L, 50L},
                new Object[]{1L, 60L},
                new Object[]{1L, 70L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.70, assessment.utilization(), 0.0001);
        assertEquals(4, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.CRITICAL, assessment.level());
    }

    @Test
    void shouldResetStreakAfterRecoveryWeek() {

        when(lifeArea.getId()).thenReturn(1L);

        Week weekOne = createWeek(
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 30)
        );

        Week weekTwo = createWeek(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 6)
        );

        Week weekThree = createWeek(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13)
        );

        mockMultipleWeeks(
                List.of(weekThree, weekTwo, weekOne),
                List.of(
                        createAllocation(weekOne, 100),
                        createAllocation(weekTwo, 100),
                        createAllocation(weekThree, 100)
                ),
                new Object[]{1L, 40L},
                new Object[]{1L, 60L},
                new Object[]{1L, 90L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.90, assessment.utilization(), 0.0001);
        assertEquals(0, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, assessment.level());
    }

    @Test
    void shouldReturnEmptyResultWhenThereAreNoCompletedWeeks() {

        when(weekService.getLatestCompletedWeeks())
                .thenReturn(List.of());

        List<NeglectAssessment> result =
                neglectService.calculate();

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(weeklyAllocationRepository, never())
                .findAllByWeekIdOrderByLifeAreaIdAsc(any());

        verify(timeLogRepository, never())
                .sumDurationByLifeArea(
                        anyLong(),
                        any(LocalDate.class),
                        any(LocalDate.class)
                );
    }

    @Test
    void shouldReturnEmptyResultWhenCompletedWeekHasNoAllocations() {

        Week week = createWeek(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20)
        );

        when(weekService.getLatestCompletedWeeks())
                .thenReturn(List.of(week));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of());

        List<NeglectAssessment> result =
                neglectService.calculate();

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(timeLogRepository, never())
                .sumDurationByLifeArea(
                        anyLong(),
                        any(LocalDate.class),
                        any(LocalDate.class)
                );
    }

    @Test
    void shouldNotCountExactlyEightyPercentAsUnderTarget() {

        when(lifeArea.getId()).thenReturn(1L);

        Week week = createWeek(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20)
        );

        WeeklyAllocation allocation =
                createAllocation(week, 100);

        mockWeekData(
                week,
                allocation,
                new Object[]{1L, 80L}
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.80, assessment.utilization(), 0.0001);
        assertEquals(0, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, assessment.level());
    }

    @Test
    void shouldUseCurrentUserForCrossUserDataIsolation() {

        when(lifeArea.getId()).thenReturn(1L);

        Week userWeek = createWeek(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20)
        );

        WeeklyAllocation allocation =
                createAllocation(userWeek, 100);

        when(weekService.getLatestCompletedWeeks())
                .thenReturn(List.of(userWeek));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(userWeek.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                userWeek.getWeekStartDate(),
                userWeek.getWeekEndDate()
        )).thenReturn(
                List.<Object[]>of(
                        new Object[]{1L, 40L}
                )
        );

        List<NeglectAssessment> result =
                neglectService.calculate();

        NeglectAssessment assessment =
                getSingleAssessment(result);

        assertEquals(1L, assessment.lifeAreaId());
        assertEquals(0.40, assessment.utilization(), 0.0001);
        assertEquals(1, assessment.consecutiveUnderTargetWeeks());
        assertEquals(NeglectLevel.NORMAL, assessment.level());

        verify(weekService)
                .getLatestCompletedWeeks();

        verify(timeLogRepository)
                .sumDurationByLifeArea(
                        currentUser.getId(),
                        userWeek.getWeekStartDate(),
                        userWeek.getWeekEndDate()
                );
    }

    private NeglectAssessment getSingleAssessment(
            List<NeglectAssessment> result
    ) {
        assertNotNull(result);
        assertEquals(1, result.size());
        return result.get(0);
    }

    private Week createWeek(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new Week(
                currentUser,
                startDate,
                endDate,
                10080,
                7200,
                null
        );
    }

    private WeeklyAllocation createAllocation(
            Week week,
            int recommendedMinutes
    ) {
        return new WeeklyAllocation(
                week,
                lifeArea,
                recommendedMinutes,
                recommendedMinutes,
                0,
                "v1",
                null
        );
    }

    private void mockWeekData(
            Week week,
            WeeklyAllocation allocation,
            Object[]... actualRows
    ) {
        when(weekService.getLatestCompletedWeeks())
                .thenReturn(List.of(week));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                week.getWeekStartDate(),
                week.getWeekEndDate()
        )).thenReturn(
                List.of(actualRows)
        );
    }

    private void mockMultipleWeeks(
            List<Week> weeks,
            List<WeeklyAllocation> allocations,
            Object[]... actualRows
    ) {
        when(weekService.getLatestCompletedWeeks())
                .thenReturn(weeks);

        for (int i = 0; i < weeks.size(); i++) {

            Week week = weeks.get(i);

            /*
             * Test data is supplied oldest -> newest,
             * while WeekService returns newest -> oldest.
             */
            int chronologicalIndex = weeks.size() - 1 - i;

            WeeklyAllocation allocation =
                    allocations.get(chronologicalIndex);

            when(weeklyAllocationRepository
                    .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                    .thenReturn(List.of(allocation));

            when(timeLogRepository.sumDurationByLifeArea(
                    currentUser.getId(),
                    week.getWeekStartDate(),
                    week.getWeekEndDate()
            )).thenReturn(
                    List.<Object[]>of(
                            actualRows[chronologicalIndex]
                    )
            );
        }
    }
}