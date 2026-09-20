package com.weekahead.allocation.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AllocationEngine {

    public AllocationResult calculate(AllocationInput input) {

        validateInput(input);

        int discretionaryMinutes =
                input.availableMinutes() - input.fixedCommitmentMinutes();

        validateMinimums(input.lifeAreas(), discretionaryMinutes);

        List<WorkingAllocation> workingAllocations =
                initializeAllocations(input.lifeAreas());

        int remainingMinutes =
                discretionaryMinutes
                        - workingAllocations.stream()
                        .mapToInt(WorkingAllocation::allocatedMinutes)
                        .sum();

        while (remainingMinutes > 0) {

            List<WorkingAllocation> eligible =
                    workingAllocations.stream()
                            .filter(WorkingAllocation::canReceiveMore)
                            .toList();

            if (eligible.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unable to allocate all discretionary minutes within maximum limits"
                );
            }

            long totalWeight = eligible.stream()
                    .mapToLong(allocation ->
                            allocation.input().weight()
                    )
                    .sum();

            List<WeightedShare> shares = new ArrayList<>();

            for (WorkingAllocation allocation : eligible) {

                long numerator =
                        (long) remainingMinutes
                                * allocation.input().weight();

                long floorShare = numerator / totalWeight;
                long remainder = numerator % totalWeight;

                int capacity =
                        allocation.input().maxMinutes()
                                - allocation.allocatedMinutes();

                int share = (int) Math.min(
                        floorShare,
                        capacity
                );

                shares.add(
                        new WeightedShare(
                                allocation,
                                share,
                                remainder
                        )
                );
            }

            int distributed = 0;

            for (WeightedShare share : shares) {
                share.allocation().addMinutes(share.floorShare());
                distributed += share.floorShare();
            }

            remainingMinutes -= distributed;

            if (remainingMinutes > 0) {

                shares.stream()
                        .filter(share ->
                                share.allocation().canReceiveMore()
                        )
                        .sorted(
                                Comparator
                                        .comparingLong(
                                                WeightedShare::remainder
                                        )
                                        .reversed()
                                        .thenComparing(
                                                share -> share.allocation()
                                                        .input()
                                                        .lifeAreaId()
                                        )
                        )
                        .limit(remainingMinutes)
                        .forEach(share -> {
                            share.allocation().addMinutes(1);
                        });

                int redistributed = Math.min(
                        remainingMinutes,
                        (int) shares.stream()
                                .filter(share ->
                                        share.allocation().canReceiveMore()
                                )
                                .count()
                );

                remainingMinutes -= redistributed;
            }
        }

        List<AllocationResultItem> resultItems =
                workingAllocations.stream()
                        .map(allocation ->
                                new AllocationResultItem(
                                        allocation.input().lifeAreaId(),
                                        allocation.input().name(),
                                        allocation.input().weight(),
                                        allocation.input().minMinutes(),
                                        allocation.input().maxMinutes(),
                                        allocation.allocatedMinutes()
                                )
                        )
                        .toList();

        int totalRecommendedMinutes =
                resultItems.stream()
                        .mapToInt(AllocationResultItem::recommendedMinutes)
                        .sum();

        if (totalRecommendedMinutes != discretionaryMinutes) {
            throw new IllegalStateException(
                    "Allocation total does not match discretionary minutes"
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
                    "At least one life area is required"
            );
        }

        for (LifeAreaAllocationInput lifeArea : input.lifeAreas()) {

            if (lifeArea == null) {
                throw new IllegalArgumentException(
                        "Life area cannot be null"
                );
            }

            if (lifeArea.lifeAreaId() == null) {
                throw new IllegalArgumentException(
                        "Life area id is required"
                );
            }

            if (lifeArea.weight() == null
                    || lifeArea.weight() <= 0) {
                throw new IllegalArgumentException(
                        "Life area weight must be greater than 0"
                );
            }

            if (lifeArea.minMinutes() == null
                    || lifeArea.minMinutes() < 0) {
                throw new IllegalArgumentException(
                        "Life area minimum minutes must be at least 0"
                );
            }

            if (lifeArea.maxMinutes() == null
                    || lifeArea.maxMinutes() < lifeArea.minMinutes()) {
                throw new IllegalArgumentException(
                        "Life area maximum minutes cannot be less than minimum minutes"
                );
            }
        }
    }

    private void validateMinimums(
            List<LifeAreaAllocationInput> lifeAreas,
            int discretionaryMinutes
    ) {

        long totalMinimums = lifeAreas.stream()
                .mapToLong(LifeAreaAllocationInput::minMinutes)
                .sum();

        if (totalMinimums > discretionaryMinutes) {
            throw new IllegalArgumentException(
                    "Life area minimum minutes exceed discretionary minutes"
            );
        }
    }

    private List<WorkingAllocation> initializeAllocations(
            List<LifeAreaAllocationInput> inputs
    ) {

        return inputs.stream()
                .sorted(
                        Comparator.comparing(
                                LifeAreaAllocationInput::lifeAreaId
                        )
                )
                .map(input ->
                        new WorkingAllocation(
                                input,
                                input.minMinutes()
                        )
                )
                .toList();
    }

    private record WeightedShare(
            WorkingAllocation allocation,
            int floorShare,
            long remainder
    ) {
    }

    private static final class WorkingAllocation {

        private final LifeAreaAllocationInput input;
        private int allocatedMinutes;

        private WorkingAllocation(
                LifeAreaAllocationInput input,
                int allocatedMinutes
        ) {
            this.input = input;
            this.allocatedMinutes = allocatedMinutes;
        }

        private LifeAreaAllocationInput input() {
            return input;
        }

        private int allocatedMinutes() {
            return allocatedMinutes;
        }

        private void addMinutes(int minutes) {
            allocatedMinutes += minutes;
        }

        private boolean canReceiveMore() {
            return allocatedMinutes < input.maxMinutes();
        }
    }
}