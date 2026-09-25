package com.weekahead.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.repository.WeekRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class AnalyticsCacheIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

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

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = org.mockito.Mockito.mock(User.class);

        when(currentUser.getId()).thenReturn(1L);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        1L,
                        LocalDate.now(),
                        PageRequest.of(0, 12)
                ))
                .thenReturn(List.of());

        when(weekRepository
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        1L,
                        LocalDate.now(),
                        PageRequest.of(0, 6)
                ))
                .thenReturn(List.of());

        Cache cache = cacheManager.getCache("analytics");

        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldUseCacheForSecondAnalyticsRequest() {
        var firstResponse = analyticsService.getAnalytics(12);

        var secondResponse = analyticsService.getAnalytics(12);

        assertEquals(firstResponse, secondResponse);

        verify(weekRepository, times(1))
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        1L,
                        LocalDate.now(),
                        PageRequest.of(0, 12)
                );
    }

    @Test
    void shouldUseDifferentCacheEntryForDifferentWeeks() {
        analyticsService.getAnalytics(12);
        analyticsService.getAnalytics(6);

        verify(weekRepository, times(1))
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        1L,
                        LocalDate.now(),
                        PageRequest.of(0, 12)
                );

        verify(weekRepository, times(1))
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        1L,
                        LocalDate.now(),
                        PageRequest.of(0, 6)
                );
    }

    @Test
    void shouldCreateAnalyticsCacheEntry() {
        var response = analyticsService.getAnalytics(12);

        Cache cache = cacheManager.getCache("analytics");

        assertNotNull(cache);

        Cache.ValueWrapper cachedValue =
                cache.get("1:12");

        assertNotNull(cachedValue);
        assertEquals(response, cachedValue.get());
    }
}