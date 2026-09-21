package com.weekahead.dashboard.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.dashboard.dto.DashboardLifeAreaResponse;
import com.weekahead.dashboard.dto.WeeklyDashboardResponse;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final WeekRepository weekRepository;
    private final WeeklyAllocationRepository weeklyAllocationRepository;
    private final TimeLogRepository timeLogRepository;
    private final CurrentUserService currentUserService;

    public DashboardService(
            WeekRepository weekRepository,
            WeeklyAllocationRepository weeklyAllocationRepository,
            TimeLogRepository timeLogRepository,
            CurrentUserService currentUserService
    ) {
        this.weekRepository = weekRepository;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.timeLogRepository = timeLogRepository;
        this.currentUserService = currentUserService;
    }

    public WeeklyDashboardResponse getWeeklyDashboard() {
        User currentUser = currentUserService.getCurrentUser();

        LocalDate today = LocalDate.now();

        Week week = weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        currentUser.getId(),
                        today,
                        today
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Current week not found"
                ));

        List<WeeklyAllocation> allocations =
                weeklyAllocationRepository.findAllByWeekIdOrderByLifeAreaIdAsc(
                        week.getId()
                );

        Map<Long, Integer> actualMinutesByLifeArea =
                getActualMinutesByLifeArea(
                        currentUser.getId(),
                        week.getWeekStartDate(),
                        week.getWeekEndDate()
                );

        List<DashboardLifeAreaResponse> lifeAreas =
                allocations.stream()
                        .map(allocation -> toLifeAreaResponse(
                                allocation,
                                actualMinutesByLifeArea
                        ))
                        .toList();

        int discretionaryMinutes =
                week.getAvailableMinutes()
                        - week.getFixedCommitmentMinutes();

        int totalRecommendedMinutes = lifeAreas.stream()
                .mapToInt(DashboardLifeAreaResponse::recommendedMinutes)
                .sum();

        int totalActualMinutes = lifeAreas.stream()
                .mapToInt(DashboardLifeAreaResponse::actualMinutes)
                .sum();

        int totalDeficitMinutes = lifeAreas.stream()
                .mapToInt(DashboardLifeAreaResponse::deficitMinutes)
                .sum();

        int totalOverflowMinutes = lifeAreas.stream()
                .mapToInt(DashboardLifeAreaResponse::overflowMinutes)
                .sum();

        return new WeeklyDashboardResponse(
                week.getId(),
                week.getWeekStartDate(),
                week.getWeekEndDate(),
                week.getAvailableMinutes(),
                week.getFixedCommitmentMinutes(),
                discretionaryMinutes,
                totalRecommendedMinutes,
                totalActualMinutes,
                totalDeficitMinutes,
                totalOverflowMinutes,
                lifeAreas
        );
    }

    private Map<Long, Integer> getActualMinutesByLifeArea(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        Map<Long, Integer> actualMinutesByLifeArea = new HashMap<>();

        List<Object[]> results =
                timeLogRepository.sumDurationByLifeArea(
                        userId,
                        from,
                        to
                );

        for (Object[] result : results) {
            Long lifeAreaId = ((Number) result[0]).longValue();
            Integer totalMinutes = ((Number) result[1]).intValue();

            actualMinutesByLifeArea.put(
                    lifeAreaId,
                    totalMinutes
            );
        }

        return actualMinutesByLifeArea;
    }

    private DashboardLifeAreaResponse toLifeAreaResponse(
            WeeklyAllocation allocation,
            Map<Long, Integer> actualMinutesByLifeArea
    ) {
        Long lifeAreaId = allocation.getLifeArea().getId();

        int recommendedMinutes = allocation.getRecommendedMinutes();

        int actualMinutes = actualMinutesByLifeArea.getOrDefault(
                lifeAreaId,
                0
        );

        int deficitMinutes = Math.max(
                recommendedMinutes - actualMinutes,
                0
        );

        int overflowMinutes = Math.max(
                actualMinutes - recommendedMinutes,
                0
        );

        return new DashboardLifeAreaResponse(
                lifeAreaId,
                allocation.getLifeArea().getName(),
                recommendedMinutes,
                actualMinutes,
                deficitMinutes,
                overflowMinutes,
                false
        );
    }
}