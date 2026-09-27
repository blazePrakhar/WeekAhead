package com.weekahead.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.dashboard.dto.WeeklyDashboardResponse;
import com.weekahead.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/weekly")
    public WeeklyDashboardResponse getWeeklyDashboard() {
        return dashboardService.getWeeklyDashboard();
    }
}