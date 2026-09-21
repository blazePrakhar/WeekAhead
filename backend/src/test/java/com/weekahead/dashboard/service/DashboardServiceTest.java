package com.weekahead.dashboard.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    private DashboardService dashboardService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                weekRepository,
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
    }

    private LifeArea mockLifeArea(
            Long id,
            String name
    ) {
        LifeArea lifeArea = mock(LifeArea.class);

        when(lifeArea.getId()).thenReturn(id);
        when(lifeArea.getName()).thenReturn(name);

        return lifeArea;
    }

    private LocalDate getCurrentWeekStartDate() {
        return LocalDate.now().minusDays(
                LocalDate.now().getDayOfWeek().getValue() - 1
        );
    }

    private Week createCurrentWeek(
            LocalDate startDate,
            LocalDate endDate,
            int availableMinutes,
            int fixedCommitmentMinutes
    ) {
        return new Week(
                currentUser,
                startDate,
                endDate,
                availableMinutes,
                fixedCommitmentMinutes,
                null
        );
    }

    private void mockCurrentWeek(Week week) {
        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        currentUser.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.of(week));
    }

    @Test
    void shouldBuildWeeklyDashboard() {
        LocalDate startDate = getCurrentWeekStartDate();
        LocalDate endDate = startDate.plusDays(6);

        Week week = createCurrentWeek(
                startDate,
                endDate,
                10080,
                7200
        );

        LifeArea programming = mockLifeArea(
                1L,
                "Programming"
        );

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                programming,
                300,
                300,
                0,
                "v1",
                "Test allocation"
        );

        mockCurrentWeek(week);

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                startDate,
                endDate
        )).thenReturn(List.<Object[]>of(
                new Object[]{1L, 240L}
        ));

        var response = dashboardService.getWeeklyDashboard();

        assertEquals(week.getId(), response.weekId());
        assertEquals(startDate, response.weekStartDate());
        assertEquals(endDate, response.weekEndDate());

        assertEquals(10080, response.availableMinutes());
        assertEquals(7200, response.fixedCommitmentMinutes());
        assertEquals(2880, response.discretionaryMinutes());

        assertEquals(300, response.totalRecommendedMinutes());
        assertEquals(240, response.totalActualMinutes());

        assertEquals(60, response.totalDeficitMinutes());
        assertEquals(0, response.totalOverflowMinutes());

        assertEquals(1, response.lifeAreas().size());

        var lifeArea = response.lifeAreas().get(0);

        assertEquals("Programming", lifeArea.lifeAreaName());
        assertEquals(300, lifeArea.recommendedMinutes());
        assertEquals(240, lifeArea.actualMinutes());
        assertEquals(60, lifeArea.deficitMinutes());
        assertEquals(0, lifeArea.overflowMinutes());
        assertEquals(false, lifeArea.neglected());
    }

    @Test
    void shouldReturnZeroActualMinutesWhenThereAreNoTimeLogs() {
        LocalDate startDate = getCurrentWeekStartDate();
        LocalDate endDate = startDate.plusDays(6);

        Week week = createCurrentWeek(
                startDate,
                endDate,
                10080,
                7200
        );

        LifeArea programming = mockLifeArea(
                1L,
                "Programming"
        );

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                programming,
                300,
                300,
                0,
                "v1",
                "Test allocation"
        );

        mockCurrentWeek(week);

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                startDate,
                endDate
        )).thenReturn(List.of());

        var response = dashboardService.getWeeklyDashboard();

        var lifeArea = response.lifeAreas().get(0);

        assertEquals(0, lifeArea.actualMinutes());
        assertEquals(300, lifeArea.deficitMinutes());
        assertEquals(0, lifeArea.overflowMinutes());

        assertEquals(0, response.totalActualMinutes());
        assertEquals(300, response.totalDeficitMinutes());
        assertEquals(0, response.totalOverflowMinutes());
    }

    @Test
    void shouldCalculateOverflowWhenActualExceedsRecommendation() {
        LocalDate startDate = getCurrentWeekStartDate();
        LocalDate endDate = startDate.plusDays(6);

        Week week = createCurrentWeek(
                startDate,
                endDate,
                10080,
                7200
        );

        LifeArea football = mockLifeArea(
                1L,
                "Football"
        );

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                football,
                300,
                300,
                0,
                "v1",
                "Test allocation"
        );

        mockCurrentWeek(week);

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                startDate,
                endDate
        )).thenReturn(List.<Object[]>of(
                new Object[]{1L, 420L}
        ));

        var response = dashboardService.getWeeklyDashboard();

        var lifeArea = response.lifeAreas().get(0);

        assertEquals(420, lifeArea.actualMinutes());
        assertEquals(0, lifeArea.deficitMinutes());
        assertEquals(120, lifeArea.overflowMinutes());

        assertEquals(420, response.totalActualMinutes());
        assertEquals(0, response.totalDeficitMinutes());
        assertEquals(120, response.totalOverflowMinutes());
    }

    @Test
    void shouldAggregateMultipleLifeAreas() {
        LocalDate startDate = getCurrentWeekStartDate();
        LocalDate endDate = startDate.plusDays(6);

        Week week = createCurrentWeek(
                startDate,
                endDate,
                10080,
                6000
        );

        LifeArea programming = mockLifeArea(
                1L,
                "Programming"
        );

        LifeArea football = mockLifeArea(
                2L,
                "Football"
        );

        WeeklyAllocation programmingAllocation = new WeeklyAllocation(
                week,
                programming,
                300,
                300,
                0,
                "v1",
                "Programming allocation"
        );

        WeeklyAllocation footballAllocation = new WeeklyAllocation(
                week,
                football,
                240,
                240,
                0,
                "v1",
                "Football allocation"
        );

        mockCurrentWeek(week);

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(
                        programmingAllocation,
                        footballAllocation
                ));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                startDate,
                endDate
        )).thenReturn(List.<Object[]>of(
                new Object[]{1L, 240L},
                new Object[]{2L, 300L}
        ));

        var response = dashboardService.getWeeklyDashboard();

        assertEquals(2, response.lifeAreas().size());

        assertEquals(540, response.totalRecommendedMinutes());
        assertEquals(540, response.totalActualMinutes());

        assertEquals(60, response.totalDeficitMinutes());
        assertEquals(60, response.totalOverflowMinutes());
    }

    @Test
    void shouldHandleZeroRecommendedMinutes() {
        LocalDate startDate = getCurrentWeekStartDate();
        LocalDate endDate = startDate.plusDays(6);

        Week week = createCurrentWeek(
                startDate,
                endDate,
                10080,
                7200
        );

        LifeArea programming = mockLifeArea(
                1L,
                "Programming"
        );

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                programming,
                0,
                0,
                0,
                "v1",
                "Zero recommendation"
        );

        mockCurrentWeek(week);

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                startDate,
                endDate
        )).thenReturn(List.<Object[]>of(
                new Object[]{1L, 60L}
        ));

        var response = dashboardService.getWeeklyDashboard();

        var lifeArea = response.lifeAreas().get(0);

        assertEquals(0, lifeArea.recommendedMinutes());
        assertEquals(60, lifeArea.actualMinutes());
        assertEquals(0, lifeArea.deficitMinutes());
        assertEquals(60, lifeArea.overflowMinutes());

        assertEquals(0, response.totalRecommendedMinutes());
        assertEquals(60, response.totalActualMinutes());
        assertEquals(0, response.totalDeficitMinutes());
        assertEquals(60, response.totalOverflowMinutes());
    }

    @Test
    void shouldHaveNoDeficitOrOverflowWhenActualMatchesRecommendation() {
        LocalDate startDate = getCurrentWeekStartDate();
        LocalDate endDate = startDate.plusDays(6);

        Week week = createCurrentWeek(
                startDate,
                endDate,
                10080,
                7200
        );

        LifeArea programming = mockLifeArea(
                1L,
                "Programming"
        );

        WeeklyAllocation allocation = new WeeklyAllocation(
                week,
                programming,
                300,
                300,
                0,
                "v1",
                "Matching allocation"
        );

        mockCurrentWeek(week);

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId()))
                .thenReturn(List.of(allocation));

        when(timeLogRepository.sumDurationByLifeArea(
                currentUser.getId(),
                startDate,
                endDate
        )).thenReturn(List.<Object[]>of(
                new Object[]{1L, 300L}
        ));

        var response = dashboardService.getWeeklyDashboard();

        var lifeArea = response.lifeAreas().get(0);

        assertEquals(300, lifeArea.recommendedMinutes());
        assertEquals(300, lifeArea.actualMinutes());
        assertEquals(0, lifeArea.deficitMinutes());
        assertEquals(0, lifeArea.overflowMinutes());

        assertEquals(300, response.totalRecommendedMinutes());
        assertEquals(300, response.totalActualMinutes());
        assertEquals(0, response.totalDeficitMinutes());
        assertEquals(0, response.totalOverflowMinutes());
    }

    @Test
    void shouldRejectWhenCurrentWeekDoesNotExist() {
        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        currentUser.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.getWeeklyDashboard()
        );

        assertEquals(
                "Current week not found",
                exception.getMessage()
        );
    }
}
