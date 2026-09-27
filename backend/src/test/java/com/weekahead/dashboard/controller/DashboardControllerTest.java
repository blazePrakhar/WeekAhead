package com.weekahead.dashboard.controller;

import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.auth.service.JwtService;
import com.weekahead.dashboard.dto.DashboardLifeAreaResponse;
import com.weekahead.dashboard.dto.WeeklyDashboardResponse;
import com.weekahead.dashboard.service.DashboardService;
import com.weekahead.rebalancing.model.RebalancingSuggestion;

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

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldGetWeeklyDashboard() throws Exception {
        WeeklyDashboardResponse response = new WeeklyDashboardResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                4200,
                600,
                3600,
                540,
                480,
                480,
                60,
                0,
                List.of(),
                List.of()
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekId").value(1))
                .andExpect(jsonPath("$.weekStartDate").value("2026-09-21"))
                .andExpect(jsonPath("$.weekEndDate").value("2026-09-27"))
                .andExpect(jsonPath("$.availableMinutes").value(4200))
                .andExpect(jsonPath("$.fixedCommitmentMinutes").value(600))
                .andExpect(jsonPath("$.discretionaryMinutes").value(3600))
                .andExpect(jsonPath("$.totalRecommendedMinutes").value(540))
                .andExpect(jsonPath("$.totalPlannedMinutes").value(480))
                .andExpect(jsonPath("$.totalActualMinutes").value(480))
                .andExpect(jsonPath("$.totalDeficitMinutes").value(60))
                .andExpect(jsonPath("$.totalOverflowMinutes").value(0))
                .andExpect(jsonPath("$.lifeAreas").isArray())
                .andExpect(jsonPath("$.lifeAreas.length()").value(0))
                .andExpect(jsonPath("$.rebalancingSuggestions").isArray())
                .andExpect(jsonPath("$.rebalancingSuggestions.length()").value(0));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnDashboardWithLifeAreaBreakdown() throws Exception {
        DashboardLifeAreaResponse lifeArea = new DashboardLifeAreaResponse(
                1L,
                "Programming",
                300,
                240,
                240,
                60,
                0,
                false
        );

        WeeklyDashboardResponse response = new WeeklyDashboardResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                4200,
                600,
                3600,
                300,
                240,
                240,
                60,
                0,
                List.of(lifeArea),
                List.of()
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeAreas[0].lifeAreaId").value(1))
                .andExpect(jsonPath("$.lifeAreas[0].lifeAreaName").value("Programming"))
                .andExpect(jsonPath("$.lifeAreas[0].recommendedMinutes").value(300))
                .andExpect(jsonPath("$.lifeAreas[0].plannedMinutes").value(240))
                .andExpect(jsonPath("$.lifeAreas[0].actualMinutes").value(240))
                .andExpect(jsonPath("$.lifeAreas[0].deficitMinutes").value(60))
                .andExpect(jsonPath("$.lifeAreas[0].overflowMinutes").value(0))
                .andExpect(jsonPath("$.lifeAreas[0].neglected").value(false));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnNeglectedLifeArea() throws Exception {
        DashboardLifeAreaResponse lifeArea = new DashboardLifeAreaResponse(
                1L,
                "Programming",
                300,
                240,
                100,
                200,
                0,
                true
        );

        WeeklyDashboardResponse response = new WeeklyDashboardResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                4200,
                600,
                3600,
                300,
                240,
                100,
                200,
                0,
                List.of(lifeArea),
                List.of()
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeAreas[0].lifeAreaId").value(1))
                .andExpect(jsonPath("$.lifeAreas[0].neglected").value(true))
                .andExpect(jsonPath("$.lifeAreas[0].plannedMinutes").value(240));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnRebalancingSuggestions() throws Exception {
        RebalancingSuggestion suggestion = new RebalancingSuggestion(
                1L,
                2L,
                60,
                "60 minutes can be transferred from Programming to Football."
        );

        WeeklyDashboardResponse response = new WeeklyDashboardResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                4200,
                600,
                3600,
                540,
                540,
                480,
                60,
                0,
                List.of(),
                List.of(suggestion)
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rebalancingSuggestions").isArray())
                .andExpect(jsonPath("$.rebalancingSuggestions.length()").value(1))
                .andExpect(jsonPath("$.rebalancingSuggestions[0].sourceLifeAreaId").value(1))
                .andExpect(jsonPath("$.rebalancingSuggestions[0].destinationLifeAreaId").value(2))
                .andExpect(jsonPath("$.rebalancingSuggestions[0].transferableMinutes").value(60))
                .andExpect(jsonPath("$.rebalancingSuggestions[0].explanation")
                        .value("60 minutes can be transferred from Programming to Football."));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnOverflowInLifeAreaBreakdown() throws Exception {
        DashboardLifeAreaResponse lifeArea = new DashboardLifeAreaResponse(
                2L,
                "Football",
                240,
                300,
                300,
                0,
                60,
                false
        );

        WeeklyDashboardResponse response = new WeeklyDashboardResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                4200,
                600,
                3600,
                240,
                300,
                300,
                0,
                60,
                List.of(lifeArea),
                List.of()
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecommendedMinutes").value(240))
                .andExpect(jsonPath("$.totalPlannedMinutes").value(300))
                .andExpect(jsonPath("$.totalActualMinutes").value(300))
                .andExpect(jsonPath("$.totalDeficitMinutes").value(0))
                .andExpect(jsonPath("$.totalOverflowMinutes").value(60))
                .andExpect(jsonPath("$.lifeAreas[0].plannedMinutes").value(300))
                .andExpect(jsonPath("$.lifeAreas[0].overflowMinutes").value(60))
                .andExpect(jsonPath("$.lifeAreas[0].deficitMinutes").value(0));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnEmptyLifeAreasWhenThereAreNoAllocations() throws Exception {
        WeeklyDashboardResponse response = new WeeklyDashboardResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                4200,
                600,
                3600,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of()
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeAreas").isArray())
                .andExpect(jsonPath("$.lifeAreas.length()").value(0))
                .andExpect(jsonPath("$.totalRecommendedMinutes").value(0))
                .andExpect(jsonPath("$.totalPlannedMinutes").value(0))
                .andExpect(jsonPath("$.totalActualMinutes").value(0))
                .andExpect(jsonPath("$.rebalancingSuggestions").isArray())
                .andExpect(jsonPath("$.rebalancingSuggestions.length()").value(0));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldRejectUnsupportedPostRequest() throws Exception {
        mockMvc.perform(post("/api/dashboard/weekly"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(dashboardService);
    }
}