package com.weekahead.neglect.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.neglect.model.NeglectAssessment;
import com.weekahead.neglect.model.NeglectLevel;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.service.WeekService;

@Service
public class NeglectService {

    private static final double UNDER_TARGET_THRESHOLD = 0.80;

    private final WeekService weekService;
    private final WeeklyAllocationRepository weeklyAllocationRepository;
    private final TimeLogRepository timeLogRepository;
    private final CurrentUserService currentUserService;

    public NeglectService(
            WeekService weekService,
            WeeklyAllocationRepository weeklyAllocationRepository,
            TimeLogRepository timeLogRepository,
            CurrentUserService currentUserService
    ) {
        this.weekService = weekService;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.timeLogRepository = timeLogRepository;
        this.currentUserService = currentUserService;
    }

    public List<NeglectAssessment> calculate() {
        User currentUser = currentUserService.getCurrentUser();

        List<Week> completedWeeks =
                new ArrayList<>(weekService.getLatestCompletedWeeks());

        Map<Long, List<WeeklyResult>> resultsByLifeArea =
                new HashMap<>();

        for (Week week : completedWeeks) {

            List<WeeklyAllocation> allocations =
                    weeklyAllocationRepository
                            .findAllByWeekIdOrderByLifeAreaIdAsc(week.getId());

            List<Object[]> actualMinutesByLifeArea =
                    timeLogRepository.sumDurationByLifeArea(
                            currentUser.getId(),
                            week.getWeekStartDate(),
                            week.getWeekEndDate()
                    );

            Map<Long, Integer> actualMinutesMap =
                    buildActualMinutesMap(actualMinutesByLifeArea);

            for (WeeklyAllocation allocation : allocations) {

                Long lifeAreaId =
                        allocation.getLifeArea().getId();

                int recommendedMinutes =
                        allocation.getRecommendedMinutes();

                int actualMinutes =
                        actualMinutesMap.getOrDefault(
                                lifeAreaId,
                                0
                        );

                double utilization =
                        calculateUtilization(
                                actualMinutes,
                                recommendedMinutes
                        );

                boolean underTarget =
                        utilization < UNDER_TARGET_THRESHOLD;

                resultsByLifeArea
                        .computeIfAbsent(
                                lifeAreaId,
                                key -> new ArrayList<>()
                        )
                        .add(
                                new WeeklyResult(
                                        week.getWeekStartDate(),
                                        utilization,
                                        underTarget
                                )
                        );
            }
        }

        return buildAssessments(resultsByLifeArea);
    }

    private List<NeglectAssessment> buildAssessments(
            Map<Long, List<WeeklyResult>> resultsByLifeArea
    ) {
        List<NeglectAssessment> assessments =
                new ArrayList<>();

        for (Map.Entry<Long, List<WeeklyResult>> entry
                : resultsByLifeArea.entrySet()) {

            List<WeeklyResult> weeklyResults =
                    entry.getValue();

            weeklyResults.sort(
                    (first, second) ->
                            first.weekStartDate()
                                    .compareTo(second.weekStartDate())
            );

            int consecutiveUnderTargetWeeks = 0;

            for (WeeklyResult result : weeklyResults) {

                if (result.underTarget()) {
                    consecutiveUnderTargetWeeks++;
                } else {
                    consecutiveUnderTargetWeeks = 0;
                }
            }

            WeeklyResult latestResult =
                    weeklyResults.get(weeklyResults.size() - 1);

            NeglectLevel level =
                    determineLevel(consecutiveUnderTargetWeeks);

            assessments.add(
                    new NeglectAssessment(
                            entry.getKey(),
                            latestResult.utilization(),
                            consecutiveUnderTargetWeeks,
                            level
                    )
            );
        }

        return assessments;
    }

    private NeglectLevel determineLevel(
            int consecutiveUnderTargetWeeks
    ) {
        if (consecutiveUnderTargetWeeks >= 4) {
            return NeglectLevel.CRITICAL;
        }

        if (consecutiveUnderTargetWeeks >= 3) {
            return NeglectLevel.HIGH;
        }

        if (consecutiveUnderTargetWeeks >= 2) {
            return NeglectLevel.WARNING;
        }

        return NeglectLevel.NORMAL;
    }

    private Map<Long, Integer> buildActualMinutesMap(
            List<Object[]> rows
    ) {
        Map<Long, Integer> result =
                new HashMap<>();

        for (Object[] row : rows) {

            Long lifeAreaId =
                    ((Number) row[0]).longValue();

            Integer actualMinutes =
                    ((Number) row[1]).intValue();

            result.put(
                    lifeAreaId,
                    actualMinutes
            );
        }

        return result;
    }

    private double calculateUtilization(
            int actualMinutes,
            int recommendedMinutes
    ) {
        if (recommendedMinutes <= 0) {
            return 0.0;
        }

        return (double) actualMinutes / recommendedMinutes;
    }

    private record WeeklyResult(
            LocalDate weekStartDate,
            double utilization,
            boolean underTarget
    ) {
    }
}