package com.weekahead.rebalancing.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.rebalancing.model.RebalancingCalculator;
import com.weekahead.rebalancing.model.RebalancingInput;
import com.weekahead.rebalancing.model.RebalancingSuggestion;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@Service
public class RebalancingService {

    private final WeekRepository weekRepository;
    private final WeeklyAllocationRepository weeklyAllocationRepository;
    private final TimeLogRepository timeLogRepository;
    private final LifeAreaRepository lifeAreaRepository;
    private final CurrentUserService currentUserService;
    private final RebalancingCalculator rebalancingCalculator;

    public RebalancingService(
            WeekRepository weekRepository,
            WeeklyAllocationRepository weeklyAllocationRepository,
            TimeLogRepository timeLogRepository,
            LifeAreaRepository lifeAreaRepository,
            CurrentUserService currentUserService,
            RebalancingCalculator rebalancingCalculator
    ) {
        this.weekRepository = weekRepository;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.timeLogRepository = timeLogRepository;
        this.lifeAreaRepository = lifeAreaRepository;
        this.currentUserService = currentUserService;
        this.rebalancingCalculator = rebalancingCalculator;
    }

    public List<RebalancingSuggestion> calculate() {

        User currentUser =
                currentUserService.getCurrentUser();

        Week currentWeek =
                weekRepository
                        .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                                currentUser.getId(),
                                java.time.LocalDate.now(),
                                java.time.LocalDate.now()
                        )
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Current week not found"
                        ));

        List<WeeklyAllocation> allocations =
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                currentWeek.getId()
                        );

        if (allocations.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> actualMinutesByLifeArea =
                buildActualMinutesMap(
                        timeLogRepository.sumDurationByLifeArea(
                                currentUser.getId(),
                                currentWeek.getWeekStartDate(),
                                currentWeek.getWeekEndDate()
                        )
                );

        Map<Long, LifeArea> lifeAreasById =
                buildLifeAreaMap(currentUser.getId());

        List<RebalancingInput> inputs =
                allocations.stream()
                        .filter(allocation ->
                                lifeAreasById.containsKey(
                                        allocation.getLifeArea().getId()
                                )
                        )
                        .map(allocation -> {

                            LifeArea lifeArea =
                                    lifeAreasById.get(
                                            allocation.getLifeArea().getId()
                                    );

                            int actualMinutes =
                                    actualMinutesByLifeArea.getOrDefault(
                                            lifeArea.getId(),
                                            0
                                    );

                            return new RebalancingInput(
                                    lifeArea.getId(),
                                    lifeArea.getName(),
                                    allocation.getRecommendedMinutes(),
                                    actualMinutes,
                                    lifeArea.getMinMinutes(),
                                    lifeArea.getWeight()
                            );
                        })
                        .toList();

        int totalLoggedMinutes =
                actualMinutesByLifeArea.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

        int remainingWeeklyMinutes =
                Math.max(
                        currentWeek.getAvailableMinutes()
                                - currentWeek.getFixedCommitmentMinutes()
                                - totalLoggedMinutes,
                        0
                );

        return rebalancingCalculator.calculate(
                inputs,
                remainingWeeklyMinutes
        );
    }

    private Map<Long, Integer> buildActualMinutesMap(
            List<Object[]> rows
    ) {
        Map<Long, Integer> result =
                new HashMap<>();

        for (Object[] row : rows) {

            Long lifeAreaId =
                    ((Number) row[0]).longValue();

            int actualMinutes =
                    ((Number) row[1]).intValue();

            result.put(
                    lifeAreaId,
                    actualMinutes
            );
        }

        return result;
    }

    private Map<Long, LifeArea> buildLifeAreaMap(
            Long userId
    ) {
        Map<Long, LifeArea> result =
                new HashMap<>();

        for (LifeArea lifeArea :
                lifeAreaRepository.findAllByUserId(userId)) {

            result.put(
                    lifeArea.getId(),
                    lifeArea
            );
        }

        return result;
    }
}