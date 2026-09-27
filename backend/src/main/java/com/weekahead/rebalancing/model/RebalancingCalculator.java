package com.weekahead.rebalancing.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RebalancingCalculator {

    public List<RebalancingSuggestion> calculate(
            List<RebalancingInput> areas,
            int remainingWeeklyMinutes
    ) {
        if (areas == null || areas.isEmpty()) {
            return List.of();
        }

        if (remainingWeeklyMinutes <= 0) {
            return List.of();
        }

        List<AreaBalance> balances = new ArrayList<>();

        for (RebalancingInput area : areas) {

            int deficit = Math.max(
                    area.recommendedMinutes() - area.actualMinutes(),
                    0
            );

            int surplus = Math.max(
                    area.actualMinutes() - area.recommendedMinutes(),
                    0
            );

            balances.add(
                    new AreaBalance(
                            area,
                            deficit,
                            surplus
                    )
            );
        }

        List<AreaBalance> deficits =
                balances.stream()
                        .filter(balance -> balance.deficit() > 0)
                        .sorted(
                                Comparator.comparingDouble(
                                        balance ->
                                                -balance.input()
                                                        .priorityWeight()
                                )
                        )
                        .toList();

        List<AreaBalance> surpluses =
                balances.stream()
                        .filter(balance ->
                                balance.remainingSurplus() > 0)
                        .sorted(
                                Comparator.comparingDouble(
                                        balance ->
                                                balance.input()
                                                        .priorityWeight()
                                )
                        )
                        .toList();

        List<RebalancingSuggestion> suggestions =
                new ArrayList<>();

        int remainingBudget = remainingWeeklyMinutes;

        for (AreaBalance deficitArea : deficits) {

            if (remainingBudget <= 0) {
                break;
            }

            int remainingDeficit =
                    deficitArea.deficit();

            for (AreaBalance surplusArea : surpluses) {

                if (remainingBudget <= 0 ||
                        remainingDeficit <= 0) {
                    break;
                }

                if (surplusArea.remainingSurplus() <= 0) {
                    continue;
                }

                int transferableByMinimum =
                        Math.max(
                                surplusArea.input().actualMinutes()
                                        - surplusArea.input().minimumMinutes(),
                                0
                        );

                int transferableSurplus =
                        Math.min(
                                surplusArea.remainingSurplus(),
                                transferableByMinimum
                        );

                if (transferableSurplus <= 0) {
                    continue;
                }

                int transferableMinutes =
                        Math.min(
                                Math.min(
                                        transferableSurplus,
                                        remainingDeficit
                                ),
                                remainingBudget
                        );

                if (transferableMinutes <= 0) {
                    continue;
                }

                suggestions.add(
                        new RebalancingSuggestion(
                                surplusArea.input().lifeAreaId(),
                                deficitArea.input().lifeAreaId(),
                                transferableMinutes,
                                buildExplanation(
                                        surplusArea.input(),
                                        deficitArea.input(),
                                        transferableMinutes
                                )
                        )
                );

                surplusArea.reduceSurplus(
                        transferableMinutes
                );

                remainingDeficit -= transferableMinutes;
                remainingBudget -= transferableMinutes;
            }
        }

        return suggestions;
    }

    private String buildExplanation(
            RebalancingInput source,
            RebalancingInput destination,
            int transferableMinutes
    ) {
        return transferableMinutes
                + " minutes can be transferred from "
                + source.lifeAreaName()
                + " to "
                + destination.lifeAreaName()
                + " because the source is above its recommendation "
                + "and the destination is below its recommendation.";
    }

    private static class AreaBalance {

        private final RebalancingInput input;
        private final int deficit;
        private int remainingSurplus;

        private AreaBalance(
                RebalancingInput input,
                int deficit,
                int surplus
        ) {
            this.input = input;
            this.deficit = deficit;
            this.remainingSurplus = surplus;
        }

        private RebalancingInput input() {
            return input;
        }

        private int deficit() {
            return deficit;
        }

        private int remainingSurplus() {
            return remainingSurplus;
        }

        private void reduceSurplus(int minutes) {
            remainingSurplus -= minutes;
        }
    }
}