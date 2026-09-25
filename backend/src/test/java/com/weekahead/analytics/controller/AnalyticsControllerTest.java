package com.weekahead.analytics.controller;

import com.weekahead.analytics.dto.AnalyticsResponse;
import com.weekahead.analytics.dto.ConsistencyResponse;
import com.weekahead.analytics.dto.HistoricalAllocationResponse;
import com.weekahead.analytics.dto.LifeAreaComparisonResponse;
import com.weekahead.analytics.dto.WeeklyTrendResponse;
import com.weekahead.analytics.service.AnalyticsService;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.auth.service.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldGetAnalyticsWithDefaultWeeks() throws Exception {
        AnalyticsResponse response = createAnalyticsResponse();

        when(analyticsService.getAnalytics(12))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeksAnalyzed").value(2))
                .andExpect(jsonPath("$.weeklyTrends").isArray())
                .andExpect(jsonPath("$.weeklyTrends.length()").value(1))
                .andExpect(jsonPath("$.consistency.weeksAnalyzed").value(2))
                .andExpect(jsonPath("$.consistency.weeksWithActivity").value(2))
                .andExpect(jsonPath("$.consistency.totalActualMinutes").value(420))
                .andExpect(jsonPath("$.consistency.totalRecommendedMinutes").value(600))
                .andExpect(jsonPath("$.consistency.averageUtilization").value(0.7))
                .andExpect(jsonPath("$.consistency.consistentWeeks").value(1))
                .andExpect(jsonPath("$.historicalAllocation").isArray())
                .andExpect(jsonPath("$.lifeAreaComparisons").isArray());

        verify(analyticsService).getAnalytics(12);
    }

    @Test
    void shouldGetAnalyticsWithRequestedNumberOfWeeks() throws Exception {
        AnalyticsResponse response = createAnalyticsResponse();

        when(analyticsService.getAnalytics(6))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics")
                                .param("weeks", "6")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeksAnalyzed").value(2));

        verify(analyticsService).getAnalytics(6);
    }

    @Test
    void shouldReturnWeeklyTrendData() throws Exception {
        WeeklyTrendResponse trend = new WeeklyTrendResponse(
                1L,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13),
                4200,
                600,
                540,
                480,
                480,
                60,
                0
        );

        AnalyticsResponse response = new AnalyticsResponse(
                1,
                List.of(trend),
                new ConsistencyResponse(
                        1,
                        1,
                        480,
                        540,
                        0.8888888889,
                        1
                ),
                List.of(),
                List.of()
        );

        when(analyticsService.getAnalytics(12))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyTrends[0].weekId").value(1))
                .andExpect(jsonPath("$.weeklyTrends[0].weekStartDate")
                        .value("2026-09-07"))
                .andExpect(jsonPath("$.weeklyTrends[0].weekEndDate")
                        .value("2026-09-13"))
                .andExpect(jsonPath("$.weeklyTrends[0].availableMinutes")
                        .value(4200))
                .andExpect(jsonPath("$.weeklyTrends[0].fixedCommitmentMinutes")
                        .value(600))
                .andExpect(jsonPath("$.weeklyTrends[0].recommendedMinutes")
                        .value(540))
                .andExpect(jsonPath("$.weeklyTrends[0].plannedMinutes")
                        .value(480))
                .andExpect(jsonPath("$.weeklyTrends[0].actualMinutes")
                        .value(480))
                .andExpect(jsonPath("$.weeklyTrends[0].deficitMinutes")
                        .value(60))
                .andExpect(jsonPath("$.weeklyTrends[0].overflowMinutes")
                        .value(0));

        verify(analyticsService).getAnalytics(12);
    }

    @Test
    void shouldReturnLifeAreaComparisonData() throws Exception {
        LifeAreaComparisonResponse comparison =
                new LifeAreaComparisonResponse(
                        1L,
                        "Programming",
                        600,
                        540,
                        480,
                        0.8,
                        120,
                        0
                );

        AnalyticsResponse response = new AnalyticsResponse(
                1,
                List.of(),
                new ConsistencyResponse(
                        1,
                        1,
                        480,
                        600,
                        0.8,
                        1
                ),
                List.of(),
                List.of(comparison)
        );

        when(analyticsService.getAnalytics(12))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeAreaComparisons").isArray())
                .andExpect(jsonPath("$.lifeAreaComparisons.length()").value(1))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].lifeAreaId")
                        .value(1))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].lifeAreaName")
                        .value("Programming"))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].totalRecommendedMinutes")
                        .value(600))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].totalPlannedMinutes")
                        .value(540))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].totalActualMinutes")
                        .value(480))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].averageUtilization")
                        .value(0.8))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].totalDeficitMinutes")
                        .value(120))
                .andExpect(jsonPath("$.lifeAreaComparisons[0].totalOverflowMinutes")
                        .value(0));

        verify(analyticsService).getAnalytics(12);
    }

    @Test
    void shouldReturnHistoricalAllocationData() throws Exception {
        HistoricalAllocationResponse allocation =
                new HistoricalAllocationResponse(
                        1L,
                        "Programming",
                        List.of(300, 360, 420)
                );

        AnalyticsResponse response = new AnalyticsResponse(
                3,
                List.of(),
                new ConsistencyResponse(
                        3,
                        3,
                        900,
                        1080,
                        0.8333333333,
                        2
                ),
                List.of(allocation),
                List.of()
        );

        when(analyticsService.getAnalytics(12))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalAllocation").isArray())
                .andExpect(jsonPath("$.historicalAllocation.length()").value(1))
                .andExpect(jsonPath("$.historicalAllocation[0].lifeAreaId")
                        .value(1))
                .andExpect(jsonPath("$.historicalAllocation[0].lifeAreaName")
                        .value("Programming"))
                .andExpect(jsonPath(
                        "$.historicalAllocation[0].weeklyRecommendedMinutes"
                ).isArray())
                .andExpect(jsonPath(
                        "$.historicalAllocation[0].weeklyRecommendedMinutes.length()"
                ).value(3))
                .andExpect(jsonPath(
                        "$.historicalAllocation[0].weeklyRecommendedMinutes[0]"
                ).value(300))
                .andExpect(jsonPath(
                        "$.historicalAllocation[0].weeklyRecommendedMinutes[1]"
                ).value(360))
                .andExpect(jsonPath(
                        "$.historicalAllocation[0].weeklyRecommendedMinutes[2]"
                ).value(420));

        verify(analyticsService).getAnalytics(12);
    }

    @Test
    void shouldRejectUnsupportedPostRequest() throws Exception {
        mockMvc.perform(post("/api/analytics"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(analyticsService);
    }

    private AnalyticsResponse createAnalyticsResponse() {
        WeeklyTrendResponse trend = new WeeklyTrendResponse(
                1L,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 13),
                4200,
                600,
                300,
                280,
                240,
                60,
                0
        );

        ConsistencyResponse consistency =
                new ConsistencyResponse(
                        2,
                        2,
                        420,
                        600,
                        0.7,
                        1
                );

        return new AnalyticsResponse(
                2,
                List.of(trend),
                consistency,
                List.of(),
                List.of()
        );
    }
}
