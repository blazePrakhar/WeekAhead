package com.weekahead.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.neglect.service.NeglectService;
import com.weekahead.rebalancing.service.RebalancingService;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class DashboardCacheIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private WeekRepository weekRepository;

    @MockBean
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @MockBean
    private TimeLogRepository timeLogRepository;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private NeglectService neglectService;

    @MockBean
    private RebalancingService rebalancingService;

    private User currentUser;
    private Week currentWeek;

    @BeforeEach
    void setUp() {
        currentUser = org.mockito.Mockito.mock(User.class);
        currentWeek = org.mockito.Mockito.mock(Week.class);

        when(currentUser.getId()).thenReturn(1L);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(
                today.getDayOfWeek().getValue() - 1
        );
        LocalDate endDate = startDate.plusDays(6);

        when(currentWeek.getId()).thenReturn(1L);
        when(currentWeek.getWeekStartDate()).thenReturn(startDate);
        when(currentWeek.getWeekEndDate()).thenReturn(endDate);
        when(currentWeek.getAvailableMinutes()).thenReturn(10080);
        when(currentWeek.getFixedCommitmentMinutes()).thenReturn(7200);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        1L,
                        today,
                        today
                ))
                .thenReturn(Optional.of(currentWeek));

        when(weeklyAllocationRepository
                .findAllByWeekIdOrderByLifeAreaIdAsc(1L))
                .thenReturn(List.of());

        when(timeLogRepository.sumDurationByLifeArea(
                1L,
                startDate,
                endDate
        )).thenReturn(List.of());

        when(neglectService.calculate())
                .thenReturn(List.of());

        when(rebalancingService.calculate())
                .thenReturn(List.of());

        var cache = cacheManager.getCache("dashboard");

        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldUseCacheForSecondDashboardRequest() {
        var firstResponse = dashboardService.getWeeklyDashboard();

        var secondResponse = dashboardService.getWeeklyDashboard();

        assertEquals(firstResponse, secondResponse);

        verify(weekRepository, times(1))
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        1L,
                        LocalDate.now(),
                        LocalDate.now()
                );

        verify(weeklyAllocationRepository, times(1))
                .findAllByWeekIdOrderByLifeAreaIdAsc(1L);

        verify(timeLogRepository, times(1))
                .sumDurationByLifeArea(
                        1L,
                        currentWeek.getWeekStartDate(),
                        currentWeek.getWeekEndDate()
                );

        verify(neglectService, times(1))
                .calculate();

        verify(rebalancingService, times(1))
                .calculate();
    }
}