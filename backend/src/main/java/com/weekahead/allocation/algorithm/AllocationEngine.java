package com.weekahead.allocation.algorithm;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AllocationEngine {

    public AllocationResult calculate(AllocationInput input) {

        validateInput(input);

        int discretionaryMinutes
                = input.availableMinutes() - input.fixedCommitmentMinutes();

        List<LifeAreaAllocationInput> lifeAreas
                = input.lifeAreas()
                        .stream()
                        .sorted(Comparator.comparing(
                                LifeAreaAllocationInput::lifeAreaId
                        ))
                        .toList();

        int minimumTotal = lifeAreas.stream()
                .mapToInt(LifeAreaAllocationInput::minMinutes)
                .sum();

        if (minimumTotal > discretionaryMinutes) {
            throw new IllegalArgumentException(
                    "Minimum minutes exceed discretionary minutes"
            );
        }

        Map<Long, Integer> allocations = new HashMap<>();

        for (LifeAreaAllocationInput lifeArea : lifeAreas) {
            allocations.put(
                    lifeArea.lifeAreaId(),
                    lifeArea.minMinutes()
            );
        }

        int remainingMinutes
                = discretionaryMinutes - minimumTotal;

        while (remainingMinutes > 0) {

            List<LifeAreaAllocationInput> eligibleAreas
                    = lifeAreas.stream()
                            .filter(area
                                    -> allocations.get(area.lifeAreaId())
                            < area.maxMinutes()
                            )
                            .toList();

            if (eligibleAreas.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unable to allocate all discretionary minutes within maximum limits"
                );
            }

            int eligibleWeightTotal = eligibleAreas.stream()
                    .mapToInt(LifeAreaAllocationInput::weight)
                    .sum();

            Map<Long, Integer> additionalMinutes
                    = new HashMap<>();

            Map<Long, Long> remainders
                    = new HashMap<>();

            int distributedThisRound = 0;

            for (LifeAreaAllocationInput area : eligibleAreas) {

                int currentMinutes
                        = allocations.get(area.lifeAreaId());

                int remainingCapacity
                        = area.maxMinutes() - currentMinutes;

                long numerator
                        = (long) remainingMinutes * area.weight();

                int share
                        = (int) (numerator / eligibleWeightTotal);

                long remainder
                        = numerator % eligibleWeightTotal;

                share = Math.min(share, remainingCapacity);

                additionalMinutes.put(
                        area.lifeAreaId(),
                        share
                );

                remainders.put(
                        area.lifeAreaId(),
                        remainder
                );

                distributedThisRound += share;
            }

            for (LifeAreaAllocationInput area : eligibleAreas) {

                int currentMinutes
                        = allocations.get(area.lifeAreaId());

                int additional
                        = additionalMinutes.get(area.lifeAreaId());

                allocations.put(
                        area.lifeAreaId(),
                        currentMinutes + additional
                );
            }

            remainingMinutes -= distributedThisRound;

            if (remainingMinutes > 0) {

                List<LifeAreaAllocationInput> remainderOrder
                        = new ArrayList<>(eligibleAreas);

                remainderOrder.sort(
                        Comparator
                                .comparing(
                                        (LifeAreaAllocationInput area)
                                        -> remainders.get(
                                                area.lifeAreaId()
                                        )
                                )
                                .reversed()
                                .thenComparing(
                                        LifeAreaAllocationInput::lifeAreaId
                                )
                );

                boolean assignedRemainder = false;

                for (LifeAreaAllocationInput area : remainderOrder) {

                    int currentMinutes
                            = allocations.get(area.lifeAreaId());

                    if (currentMinutes < area.maxMinutes()) {

                        allocations.put(
                                area.lifeAreaId(),
                                currentMinutes + 1
                        );

                        remainingMinutes--;
                        assignedRemainder = true;

                        if (remainingMinutes == 0) {
                            break;
                        }
                    }
                }

                if (!assignedRemainder) {
                    throw new IllegalArgumentException(
                            "Unable to allocate all discretionary minutes within maximum limits"
                    );
                }
            }

            if (distributedThisRound == 0 && remainingMinutes > 0) {

                boolean assignedMinute = false;

                for (LifeAreaAllocationInput area : eligibleAreas) {

                    int currentMinutes
                            = allocations.get(area.lifeAreaId());

                    if (currentMinutes < area.maxMinutes()) {

                        allocations.put(
                                area.lifeAreaId(),
                                currentMinutes + 1
                        );

                        remainingMinutes--;
                        assignedMinute = true;
                        break;
                    }
                }

                if (!assignedMinute) {
                    throw new IllegalArgumentException(
                            "Unable to allocate all discretionary minutes within maximum limits"
                    );
                }
            }
        }

        List<AllocationResultItem> resultItems
                = lifeAreas.stream()
                        .map(area -> new AllocationResultItem(
                        area.lifeAreaId(),
                        area.name(),
                        area.weight(),
                        area.minMinutes(),
                        area.maxMinutes(),
                        allocations.get(area.lifeAreaId())
                ))
                        .toList();

        int totalRecommendedMinutes
                = resultItems.stream()
                        .mapToInt(
                                AllocationResultItem::recommendedMinutes
                        )
                        .sum();

        if (totalRecommendedMinutes != discretionaryMinutes) {
            throw new IllegalArgumentException(
                    "Unable to allocate all discretionary minutes within maximum limits"
            );
        }

        return new AllocationResult(
                discretionaryMinutes,
                totalRecommendedMinutes,
                resultItems
        );
    }

    private void validateInput(AllocationInput input) {

        if (input == null) {
            throw new IllegalArgumentException(
                    "Allocation input is required"
            );
        }

        if (input.availableMinutes() == null
                || input.availableMinutes() < 0) {
            throw new IllegalArgumentException(
                    "Available minutes must be at least 0"
            );
        }

        if (input.fixedCommitmentMinutes() == null
                || input.fixedCommitmentMinutes() < 0) {
            throw new IllegalArgumentException(
                    "Fixed commitment minutes must be at least 0"
            );
        }

        if (input.fixedCommitmentMinutes()
                > input.availableMinutes()) {
            throw new IllegalArgumentException(
                    "Fixed commitment minutes cannot exceed available minutes"
            );
        }

        if (input.lifeAreas() == null
                || input.lifeAreas().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one Life Area is required"
            );
        }

        for (LifeAreaAllocationInput area : input.lifeAreas()) {

            if (area.lifeAreaId() == null) {
                throw new IllegalArgumentException(
                        "Life Area id is required"
                );
            }

            if (area.weight() == null || area.weight() <= 0) {
                throw new IllegalArgumentException(
                        "Life Area weight must be greater than 0"
                );
            }

            if (area.minMinutes() == null
                    || area.minMinutes() < 0) {
                throw new IllegalArgumentException(
                        "Minimum minutes must be at least 0"
                );
            }

            if (area.maxMinutes() == null
                    || area.maxMinutes() < area.minMinutes()) {
                throw new IllegalArgumentException(
                        "Maximum minutes cannot be less than minimum minutes"
                );
            }
        }
    }
}
