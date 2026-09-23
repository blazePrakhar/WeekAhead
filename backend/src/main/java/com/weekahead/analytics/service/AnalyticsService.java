package com.weekahead.analytics.service;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.analytics.dto.AnalyticsResponse;
import com.weekahead.analytics.dto.ConsistencyResponse;
import com.weekahead.analytics.dto.HistoricalAllocationResponse;
import com.weekahead.analytics.dto.LifeAreaComparisonResponse;
import com.weekahead.analytics.dto.WeeklyTrendResponse;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.timetracking.entity.TimeLog;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final int DEFAULT_WEEKS = 12;
    private static final double CONSISTENCY_THRESHOLD = 0.80;

    private final CurrentUserService currentUserService;
    private final WeekRepository weekRepository;
    private final WeeklyAllocationRepository weeklyAllocationRepository;
    private final TimeLogRepository timeLogRepository;

    public AnalyticsService(
            CurrentUserService currentUserService,
            WeekRepository weekRepository,
            WeeklyAllocationRepository weeklyAllocationRepository,
            TimeLogRepository timeLogRepository
    ) {
        this.currentUserService = currentUserService;
        this.weekRepository = weekRepository;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.timeLogRepository = timeLogRepository;
    }

    public AnalyticsResponse getAnalytics() {
        return getAnalytics(DEFAULT_WEEKS);
    }

    public AnalyticsResponse getAnalytics(int weeks) {
        if (weeks <= 0) {
            throw new IllegalArgumentException("Weeks must be greater than zero");
        }

        User currentUser = currentUserService.getCurrentUser();

        LocalDate today = LocalDate.now();

        List<Week> completedWeeks =
                weekRepository.findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        currentUser.getId(),
                        today,
                        PageRequest.of(0, weeks)
                );

        completedWeeks = completedWeeks.stream()
                .sorted(Comparator.comparing(Week::getWeekStartDate))
                .toList();

        if (completedWeeks.isEmpty()) {
            return emptyResponse();
        }

        List<Long> weekIds = completedWeeks.stream()
                .map(Week::getId)
                .toList();

        List<WeeklyAllocation> allocations =
                weeklyAllocationRepository.findAllByWeekIdInOrderByWeekIdAscLifeAreaIdAsc(
                        weekIds
                );

        LocalDate from = completedWeeks.get(0).getWeekStartDate();
        LocalDate to = completedWeeks.get(completedWeeks.size() - 1).getWeekEndDate();

        List<TimeLog> timeLogs =
                timeLogRepository
                        .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                                currentUser.getId(),
                                from,
                                to
                        );

        Map<Long, List<WeeklyAllocation>> allocationsByWeek =
                groupAllocationsByWeek(allocations);

        Map<Long, Integer> actualMinutesByWeek =
                calculateActualMinutesByWeek(completedWeeks, timeLogs);

        Map<Long, Map<Long, Integer>> actualMinutesByWeekAndLifeArea =
                calculateActualMinutesByWeekAndLifeArea(completedWeeks, timeLogs);

        List<WeeklyTrendResponse> weeklyTrends =
                buildWeeklyTrends(
                        completedWeeks,
                        allocationsByWeek,
                        actualMinutesByWeek
                );

        ConsistencyResponse consistency =
                buildConsistency(
                        weeklyTrends
                );

        List<HistoricalAllocationResponse> historicalAllocation =
                buildHistoricalAllocation(
                        completedWeeks,
                        allocationsByWeek
                );

        List<LifeAreaComparisonResponse> lifeAreaComparisons =
                buildLifeAreaComparisons(
                        completedWeeks,
                        allocationsByWeek,
                        actualMinutesByWeekAndLifeArea
                );

        return new AnalyticsResponse(
                completedWeeks.size(),
                weeklyTrends,
                consistency,
                historicalAllocation,
                lifeAreaComparisons
        );
    }

    private Map<Long, List<WeeklyAllocation>> groupAllocationsByWeek(
            List<WeeklyAllocation> allocations
    ) {
        Map<Long, List<WeeklyAllocation>> result = new LinkedHashMap<>();

        for (WeeklyAllocation allocation : allocations) {
            result.computeIfAbsent(
                    allocation.getWeek().getId(),
                    ignored -> new ArrayList<>()
            ).add(allocation);
        }

        return result;
    }

    private Map<Long, Integer> calculateActualMinutesByWeek(
            List<Week> weeks,
            List<TimeLog> timeLogs
    ) {
        Map<Long, Integer> result = new LinkedHashMap<>();

        for (Week week : weeks) {
            int total = 0;

            for (TimeLog timeLog : timeLogs) {
                if (!timeLog.getLogDate().isBefore(week.getWeekStartDate())
                        && !timeLog.getLogDate().isAfter(week.getWeekEndDate())) {

                    total += safeInt(timeLog.getDurationMinutes());
                }
            }

            result.put(week.getId(), total);
        }

        return result;
    }

    private Map<Long, Map<Long, Integer>> calculateActualMinutesByWeekAndLifeArea(
            List<Week> weeks,
            List<TimeLog> timeLogs
    ) {
        Map<Long, Map<Long, Integer>> result = new LinkedHashMap<>();

        for (Week week : weeks) {
            result.put(week.getId(), new LinkedHashMap<>());
        }

        for (TimeLog timeLog : timeLogs) {
            for (Week week : weeks) {
                if (!timeLog.getLogDate().isBefore(week.getWeekStartDate())
                        && !timeLog.getLogDate().isAfter(week.getWeekEndDate())) {

                    Long lifeAreaId = timeLog.getLifeArea().getId();

                    result.get(week.getId()).merge(
                            lifeAreaId,
                            safeInt(timeLog.getDurationMinutes()),
                            Integer::sum
                    );

                    break;
                }
            }
        }

        return result;
    }

    private List<WeeklyTrendResponse> buildWeeklyTrends(
            List<Week> weeks,
            Map<Long, List<WeeklyAllocation>> allocationsByWeek,
            Map<Long, Integer> actualMinutesByWeek
    ) {
        List<WeeklyTrendResponse> result = new ArrayList<>();

        for (Week week : weeks) {
            List<WeeklyAllocation> allocations =
                    allocationsByWeek.getOrDefault(
                            week.getId(),
                            List.of()
                    );

            int recommendedMinutes = 0;
            int plannedMinutes = 0;

            for (WeeklyAllocation allocation : allocations) {
                recommendedMinutes += safeInt(
                        allocation.getRecommendedMinutes()
                );

                plannedMinutes += safeInt(
                        allocation.getPlannedMinutes()
                );
            }

            int actualMinutes =
                    actualMinutesByWeek.getOrDefault(
                            week.getId(),
                            0
                    );

            int deficitMinutes =
                    Math.max(recommendedMinutes - actualMinutes, 0);

            int overflowMinutes =
                    Math.max(actualMinutes - recommendedMinutes, 0);

            result.add(
                    new WeeklyTrendResponse(
                            week.getId(),
                            week.getWeekStartDate(),
                            week.getWeekEndDate(),
                            safeInt(week.getAvailableMinutes()),
                            safeInt(week.getFixedCommitmentMinutes()),
                            recommendedMinutes,
                            plannedMinutes,
                            actualMinutes,
                            deficitMinutes,
                            overflowMinutes
                    )
            );
        }

        return result;
    }

    private ConsistencyResponse buildConsistency(
            List<WeeklyTrendResponse> weeklyTrends
    ) {
        int weeksAnalyzed = weeklyTrends.size();

        int weeksWithActivity = 0;
        int totalActualMinutes = 0;
        int totalRecommendedMinutes = 0;
        int consistentWeeks = 0;

        for (WeeklyTrendResponse week : weeklyTrends) {
            totalActualMinutes += safeInt(week.actualMinutes());
            totalRecommendedMinutes += safeInt(week.recommendedMinutes());

            if (safeInt(week.actualMinutes()) > 0) {
                weeksWithActivity++;
            }

            double utilization =
                    week.recommendedMinutes() <= 0
                            ? 0.0
                            : (double) week.actualMinutes()
                            / week.recommendedMinutes();

            if (utilization >= CONSISTENCY_THRESHOLD) {
                consistentWeeks++;
            }
        }

        double averageUtilization =
                totalRecommendedMinutes <= 0
                        ? 0.0
                        : (double) totalActualMinutes
                        / totalRecommendedMinutes;

        return new ConsistencyResponse(
                weeksAnalyzed,
                weeksWithActivity,
                totalActualMinutes,
                totalRecommendedMinutes,
                averageUtilization,
                consistentWeeks
        );
    }

    private List<HistoricalAllocationResponse> buildHistoricalAllocation(
            List<Week> weeks,
            Map<Long, List<WeeklyAllocation>> allocationsByWeek
    ) {
        Map<Long, LifeAreaHistory> historyByLifeArea =
                new LinkedHashMap<>();

        for (Week week : weeks) {
            Map<Long, WeeklyAllocation> allocationByLifeArea =
                    new LinkedHashMap<>();

            for (WeeklyAllocation allocation :
                    allocationsByWeek.getOrDefault(
                            week.getId(),
                            List.of()
                    )) {

                allocationByLifeArea.put(
                        allocation.getLifeArea().getId(),
                        allocation
                );
            }

            for (Long lifeAreaId : historyByLifeArea.keySet()) {
                WeeklyAllocation allocation =
                        allocationByLifeArea.get(lifeAreaId);

                historyByLifeArea.get(lifeAreaId)
                        .weeklyRecommendedMinutes()
                        .add(
                                allocation == null
                                        ? 0
                                        : safeInt(
                                                allocation.getRecommendedMinutes()
                                        )
                        );
            }

            for (WeeklyAllocation allocation :
                    allocationsByWeek.getOrDefault(
                            week.getId(),
                            List.of()
                    )) {

                Long lifeAreaId = allocation.getLifeArea().getId();

                if (!historyByLifeArea.containsKey(lifeAreaId)) {
                    List<Integer> values = new ArrayList<>();

                    for (int i = 0; i < weeks.indexOf(week); i++) {
                        values.add(0);
                    }

                    values.add(
                            safeInt(
                                    allocation.getRecommendedMinutes()
                            )
                    );

                    historyByLifeArea.put(
                            lifeAreaId,
                            new LifeAreaHistory(
                                    lifeAreaId,
                                    allocation.getLifeArea().getName(),
                                    values
                            )
                    );
                }
            }
        }

        /*
         * Some life areas may have appeared only after the first
         * historical week. Pad their history so every series has
         * the same number of entries as the selected weeks.
         */
        for (LifeAreaHistory history : historyByLifeArea.values()) {
            while (history.weeklyRecommendedMinutes().size()
                    < weeks.size()) {

                history.weeklyRecommendedMinutes().add(0);
            }
        }

        return historyByLifeArea.values().stream()
                .map(history ->
                        new HistoricalAllocationResponse(
                                history.lifeAreaId(),
                                history.lifeAreaName(),
                                history.weeklyRecommendedMinutes()
                        )
                )
                .toList();
    }

    private List<LifeAreaComparisonResponse> buildLifeAreaComparisons(
            List<Week> weeks,
            Map<Long, List<WeeklyAllocation>> allocationsByWeek,
            Map<Long, Map<Long, Integer>> actualMinutesByWeekAndLifeArea
    ) {
        Map<Long, LifeAreaComparison> comparisons =
                new LinkedHashMap<>();

        for (Week week : weeks) {
            List<WeeklyAllocation> allocations =
                    allocationsByWeek.getOrDefault(
                            week.getId(),
                            List.of()
                    );

            Map<Long, Integer> actuals =
                    actualMinutesByWeekAndLifeArea.getOrDefault(
                            week.getId(),
                            Map.of()
                    );

            for (WeeklyAllocation allocation : allocations) {
                LifeArea lifeArea = allocation.getLifeArea();

                Long lifeAreaId = lifeArea.getId();

                int recommended =
                        safeInt(allocation.getRecommendedMinutes());

                int planned =
                        safeInt(allocation.getPlannedMinutes());

                int actual =
                        actuals.getOrDefault(
                                lifeAreaId,
                                0
                        );

                LifeAreaComparison comparison =
                        comparisons.computeIfAbsent(
                                lifeAreaId,
                                ignored ->
                                        new LifeAreaComparison(
                                                lifeAreaId,
                                                lifeArea.getName()
                                        )
                        );

                comparison.add(
                        recommended,
                        planned,
                        actual
                );
            }
        }

        return comparisons.values().stream()
                .map(comparison -> {
                    double averageUtilization =
                            comparison.totalRecommendedMinutes() <= 0
                                    ? 0.0
                                    : (double)
                                    comparison.totalActualMinutes()
                                    / comparison.totalRecommendedMinutes();

                    return new LifeAreaComparisonResponse(
                            comparison.lifeAreaId(),
                            comparison.lifeAreaName(),
                            comparison.totalRecommendedMinutes(),
                            comparison.totalPlannedMinutes(),
                            comparison.totalActualMinutes(),
                            averageUtilization,
                            comparison.totalDeficitMinutes(),
                            comparison.totalOverflowMinutes()
                    );
                })
                .toList();
    }

    private AnalyticsResponse emptyResponse() {
        return new AnalyticsResponse(
                0,
                List.of(),
                new ConsistencyResponse(
                        0,
                        0,
                        0,
                        0,
                        0.0,
                        0
                ),
                List.of(),
                List.of()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record LifeAreaHistory(
            Long lifeAreaId,
            String lifeAreaName,
            List<Integer> weeklyRecommendedMinutes
    ) {
    }

    private static class LifeAreaComparison {

        private final Long lifeAreaId;
        private final String lifeAreaName;

        private int totalRecommendedMinutes;
        private int totalPlannedMinutes;
        private int totalActualMinutes;
        private int totalDeficitMinutes;
        private int totalOverflowMinutes;

        private LifeAreaComparison(
                Long lifeAreaId,
                String lifeAreaName
        ) {
            this.lifeAreaId = lifeAreaId;
            this.lifeAreaName = lifeAreaName;
        }

        private void add(
                int recommended,
                int planned,
                int actual
        ) {
            totalRecommendedMinutes += recommended;
            totalPlannedMinutes += planned;
            totalActualMinutes += actual;

            totalDeficitMinutes +=
                    Math.max(recommended - actual, 0);

            totalOverflowMinutes +=
                    Math.max(actual - recommended, 0);
        }

        private Long lifeAreaId() {
            return lifeAreaId;
        }

        private String lifeAreaName() {
            return lifeAreaName;
        }

        private int totalRecommendedMinutes() {
            return totalRecommendedMinutes;
        }

        private int totalPlannedMinutes() {
            return totalPlannedMinutes;
        }

        private int totalActualMinutes() {
            return totalActualMinutes;
        }

        private int totalDeficitMinutes() {
            return totalDeficitMinutes;
        }

        private int totalOverflowMinutes() {
            return totalOverflowMinutes;
        }
    }
}