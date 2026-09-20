package com.weekahead.allocation.service;

import com.weekahead.allocation.algorithm.AllocationEngine;
import com.weekahead.allocation.algorithm.AllocationInput;
import com.weekahead.allocation.algorithm.AllocationResult;
import com.weekahead.allocation.algorithm.AllocationResultItem;
import com.weekahead.allocation.algorithm.LifeAreaAllocationInput;
import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AllocationService {

    private static final String ALGORITHM_VERSION = "v1";

    private final CurrentUserService currentUserService;
    private final WeekRepository weekRepository;
    private final LifeAreaRepository lifeAreaRepository;
    private final WeeklyAllocationRepository weeklyAllocationRepository;
    private final AllocationEngine allocationEngine;

    public AllocationService(
            CurrentUserService currentUserService,
            WeekRepository weekRepository,
            LifeAreaRepository lifeAreaRepository,
            WeeklyAllocationRepository weeklyAllocationRepository,
            AllocationEngine allocationEngine
    ) {
        this.currentUserService = currentUserService;
        this.weekRepository = weekRepository;
        this.lifeAreaRepository = lifeAreaRepository;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.allocationEngine = allocationEngine;
    }

    @Transactional
    public AllocationResult generateRecommendation(Long weekId) {

        User user = currentUserService.getCurrentUser();

        Week week = weekRepository.findByIdAndUserId(weekId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Week not found"));

        List<LifeArea> activeLifeAreas = lifeAreaRepository.findAllByUserId(user.getId())
                .stream()
                .filter(LifeArea::getIsActive)
                .toList();

        if (activeLifeAreas.isEmpty()) {
            throw new IllegalArgumentException("No active Life Areas found");
        }

        List<LifeAreaAllocationInput> lifeAreaInputs = activeLifeAreas.stream()
                .map(lifeArea -> new LifeAreaAllocationInput(
                        lifeArea.getId(),
                        lifeArea.getName(),
                        lifeArea.getWeight(),
                        lifeArea.getMinMinutes(),
                        lifeArea.getMaxMinutes()
                ))
                .toList();

        AllocationInput input = new AllocationInput(
                week.getAvailableMinutes(),
                week.getFixedCommitmentMinutes(),
                lifeAreaInputs
        );

        AllocationResult result = allocationEngine.calculate(input);

        for (AllocationResultItem item : result.allocations()) {

            LifeArea lifeArea = activeLifeAreas.stream()
                    .filter(area -> area.getId().equals(item.lifeAreaId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Life Area not found")
                    );

            WeeklyAllocation allocation =
                    weeklyAllocationRepository
                            .findByWeekIdAndLifeAreaId(weekId, lifeArea.getId())
                            .orElseGet(() -> new WeeklyAllocation(
                                    week,
                                    lifeArea,
                                    item.recommendedMinutes(),
                                    0,
                                    0,
                                    ALGORITHM_VERSION,
                                    buildExplanation(item)
                            ));

            allocation.updateRecommendation(
                    item.recommendedMinutes(),
                    ALGORITHM_VERSION,
                    buildExplanation(item)
            );

            weeklyAllocationRepository.save(allocation);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<WeeklyAllocation> getAllocations(Long weekId) {

        User user = currentUserService.getCurrentUser();

        weekRepository.findByIdAndUserId(weekId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Week not found"));

        List<WeeklyAllocation> allocations =
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(weekId);

        if (allocations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Allocation recommendation not found"
            );
        }

        return allocations;
    }

    private String buildExplanation(AllocationResultItem item) {
        return "Recommended from Life Area weight, minimum minutes, and maximum minutes.";
    }
}