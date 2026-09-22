package com.weekahead.rebalancing.model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RebalancingCalculatorTest {

    private final RebalancingCalculator calculator
            = new RebalancingCalculator();

    @Test
    void shouldTransferSurplusToDeficitArea() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        140,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        60,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 40);

        assertEquals(1, suggestions.size());

        RebalancingSuggestion suggestion
                = suggestions.get(0);

        assertEquals(1L, suggestion.sourceLifeAreaId());
        assertEquals(2L, suggestion.destinationLifeAreaId());
        assertEquals(40, suggestion.transferableMinutes());
        assertTrue(
                suggestion.explanation().contains("Health")
        );
        assertTrue(
                suggestion.explanation().contains("Learning")
        );
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoDeficit() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        140,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        120,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 60);

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoSurplus() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        80,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        70,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 60);

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void shouldNotTransferMoreThanDestinationDeficit() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        180,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        80,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 80);

        assertEquals(1, suggestions.size());
        assertEquals(
                20,
                suggestions.get(0).transferableMinutes()
        );
    }

    @Test
    void shouldNotTransferMoreThanRemainingBudget() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        180,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        200,
                        100,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 30);

        assertEquals(1, suggestions.size());
        assertEquals(
                30,
                suggestions.get(0).transferableMinutes()
        );
    }

    @Test
    void shouldPreferLowerPrioritySurplusArea() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Low Priority",
                        100,
                        150,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "High Priority",
                        100,
                        150,
                        60,
                        5.0
                ),
                new RebalancingInput(
                        3L,
                        "Learning",
                        180,
                        100,
                        60,
                        4.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 50);

        assertEquals(1, suggestions.size());

        RebalancingSuggestion suggestion
                = suggestions.get(0);

        assertEquals(
                1L,
                suggestion.sourceLifeAreaId()
        );

        assertEquals(
                3L,
                suggestion.destinationLifeAreaId()
        );

        assertEquals(
                50,
                suggestion.transferableMinutes()
        );
    }

    @Test
    void shouldRespectSourceMinimum() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        140,
                        120,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        60,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 40);

        assertEquals(1, suggestions.size());

        assertEquals(
                20,
                suggestions.get(0).transferableMinutes()
        );
    }

    @Test
    void shouldReturnEmptyWhenBudgetIsZero() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        140,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        60,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 0);

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(
                        List.of(),
                        100
                );

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void shouldHandleMultipleDeficitAreas() {

        List<RebalancingInput> areas = List.of(
                new RebalancingInput(
                        1L,
                        "Health",
                        100,
                        150,
                        60,
                        1.0
                ),
                new RebalancingInput(
                        2L,
                        "Learning",
                        100,
                        70,
                        60,
                        3.0
                ),
                new RebalancingInput(
                        3L,
                        "Career",
                        100,
                        80,
                        60,
                        2.0
                )
        );

        List<RebalancingSuggestion> suggestions
                = calculator.calculate(areas, 50);

        assertEquals(2, suggestions.size());

        // Higher-priority deficit: Learning
        assertEquals(
                2L,
                suggestions.get(0).destinationLifeAreaId()
        );

        assertEquals(
                30,
                suggestions.get(0).transferableMinutes()
        );

        // Lower-priority deficit: Career
        assertEquals(
                3L,
                suggestions.get(1).destinationLifeAreaId()
        );

        assertEquals(
                20,
                suggestions.get(1).transferableMinutes()
        );
    }
}
