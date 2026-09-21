package com.weekahead.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.auth.service.JwtService;
import com.weekahead.dashboard.dto.DashboardLifeAreaResponse;
import com.weekahead.dashboard.dto.WeeklyDashboardResponse;
import com.weekahead.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                60,
                0,
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
                .andExpect(jsonPath("$.totalActualMinutes").value(480))
                .andExpect(jsonPath("$.totalDeficitMinutes").value(60))
                .andExpect(jsonPath("$.totalOverflowMinutes").value(0))
                .andExpect(jsonPath("$.lifeAreas").isArray())
                .andExpect(jsonPath("$.lifeAreas.length()").value(0));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnDashboardWithLifeAreaBreakdown() throws Exception {
        DashboardLifeAreaResponse lifeArea = new DashboardLifeAreaResponse(
                1L,
                "Programming",
                300,
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
                60,
                0,
                List.of(lifeArea)
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeAreas[0].lifeAreaId").value(1))
                .andExpect(jsonPath("$.lifeAreas[0].lifeAreaName").value("Programming"))
                .andExpect(jsonPath("$.lifeAreas[0].recommendedMinutes").value(300))
                .andExpect(jsonPath("$.lifeAreas[0].actualMinutes").value(240))
                .andExpect(jsonPath("$.lifeAreas[0].deficitMinutes").value(60))
                .andExpect(jsonPath("$.lifeAreas[0].overflowMinutes").value(0))
                .andExpect(jsonPath("$.lifeAreas[0].neglected").value(false));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldReturnOverflowInLifeAreaBreakdown() throws Exception {
        DashboardLifeAreaResponse lifeArea = new DashboardLifeAreaResponse(
                2L,
                "Football",
                240,
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
                0,
                60,
                List.of(lifeArea)
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecommendedMinutes").value(240))
                .andExpect(jsonPath("$.totalActualMinutes").value(300))
                .andExpect(jsonPath("$.totalDeficitMinutes").value(0))
                .andExpect(jsonPath("$.totalOverflowMinutes").value(60))
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
                List.of()
        );

        when(dashboardService.getWeeklyDashboard())
                .thenReturn(response);

        mockMvc.perform(get("/api/dashboard/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeAreas").isArray())
                .andExpect(jsonPath("$.lifeAreas.length()").value(0))
                .andExpect(jsonPath("$.totalRecommendedMinutes").value(0))
                .andExpect(jsonPath("$.totalActualMinutes").value(0));

        verify(dashboardService).getWeeklyDashboard();
    }

    @Test
    void shouldRejectUnsupportedPostRequest() throws Exception {
        mockMvc.perform(post("/api/dashboard/weekly"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(dashboardService);
    }
}