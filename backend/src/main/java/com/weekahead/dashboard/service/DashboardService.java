package com.weekahead.dashboard.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.dashboard.dto.DashboardLifeAreaResponse;
import com.weekahead.dashboard.dto.WeeklyDashboardResponse;
import com.weekahead.neglect.model.NeglectAssessment;
import com.weekahead.neglect.model.NeglectLevel;
import com.weekahead.neglect.service.NeglectService;
import com.weekahead.rebalancing.model.RebalancingSuggestion;
import com.weekahead.rebalancing.service.RebalancingService;
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
    private final NeglectService neglectService;
    private final RebalancingService rebalancingService;

    public DashboardService(
            WeekRepository weekRepository,
            WeeklyAllocationRepository weeklyAllocationRepository,
            TimeLogRepository timeLogRepository,
            CurrentUserService currentUserService,
            NeglectService neglectService,
            RebalancingService rebalancingService
    ) {
        this.weekRepository = weekRepository;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.timeLogRepository = timeLogRepository;
        this.currentUserService = currentUserService;
        this.neglectService = neglectService;
        this.rebalancingService = rebalancingService;
    }

    @Cacheable(
            value = "dashboard",
            keyGenerator = "dashboardCacheKeyGenerator"
    )
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

        Map<Long, NeglectAssessment> neglectByLifeArea =
                getNeglectByLifeArea();

        List<DashboardLifeAreaResponse> lifeAreas =
                allocations.stream()
                        .map(allocation -> toLifeAreaResponse(
                                allocation,
                                actualMinutesByLifeArea,
                                neglectByLifeArea
                        ))
                        .toList();

        int discretionaryMinutes =
                week.getAvailableMinutes()
                        - week.getFixedCommitmentMinutes();

        int totalRecommendedMinutes = lifeAreas.stream()
                .mapToInt(DashboardLifeAreaResponse::recommendedMinutes)
                .sum();

        int totalPlannedMinutes = lifeAreas.stream()
                .mapToInt(DashboardLifeAreaResponse::plannedMinutes)
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

        List<RebalancingSuggestion> rebalancingSuggestions =
                rebalancingService.calculate();

        return new WeeklyDashboardResponse(
                week.getId(),
                week.getWeekStartDate(),
                week.getWeekEndDate(),
                week.getAvailableMinutes(),
                week.getFixedCommitmentMinutes(),
                discretionaryMinutes,
                totalRecommendedMinutes,
                totalPlannedMinutes,
                totalActualMinutes,
                totalDeficitMinutes,
                totalOverflowMinutes,
                lifeAreas,
                rebalancingSuggestions
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

    private Map<Long, NeglectAssessment> getNeglectByLifeArea() {
        Map<Long, NeglectAssessment> neglectByLifeArea = new HashMap<>();

        for (NeglectAssessment assessment : neglectService.calculate()) {
            neglectByLifeArea.put(
                    assessment.lifeAreaId(),
                    assessment
            );
        }

        return neglectByLifeArea;
    }

    private DashboardLifeAreaResponse toLifeAreaResponse(
            WeeklyAllocation allocation,
            Map<Long, Integer> actualMinutesByLifeArea,
            Map<Long, NeglectAssessment> neglectByLifeArea
    ) {
        Long lifeAreaId = allocation.getLifeArea().getId();

        int recommendedMinutes = allocation.getRecommendedMinutes();
        int plannedMinutes = allocation.getPlannedMinutes();

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

        NeglectAssessment neglectAssessment =
                neglectByLifeArea.get(lifeAreaId);

        boolean neglected =
                neglectAssessment != null
                        && neglectAssessment.level() != NeglectLevel.NORMAL;

        return new DashboardLifeAreaResponse(
                lifeAreaId,
                allocation.getLifeArea().getName(),
                recommendedMinutes,
                plannedMinutes,
                actualMinutes,
                deficitMinutes,
                overflowMinutes,
                neglected
        );
    }
}