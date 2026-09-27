package com.weekahead.allocation.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AllocationEngineTest {

    private final AllocationEngine engine = new AllocationEngine();

    @Test
    void shouldDistributeEqualWeightsEqually() {

        AllocationInput input = new AllocationInput(
                1000,
                200,
                List.of(
                        area(1L, "Career", 1, 0, 1000),
                        area(2L, "Fitness", 1, 0, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(800, result.discretionaryMinutes());
        assertEquals(800, result.totalRecommendedMinutes());
        assertEquals(400, result.allocations().get(0).recommendedMinutes());
        assertEquals(400, result.allocations().get(1).recommendedMinutes());
    }

    @Test
    void shouldDistributeAccordingToDifferentWeights() {

        AllocationInput input = new AllocationInput(
                1000,
                200,
                List.of(
                        area(1L, "Career", 3, 0, 1000),
                        area(2L, "Fitness", 1, 0, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(600, result.allocations().get(0).recommendedMinutes());
        assertEquals(200, result.allocations().get(1).recommendedMinutes());
        assertEquals(800, result.totalRecommendedMinutes());
    }

    @Test
    void shouldDistributeAcrossMultipleAreas() {

        AllocationInput input = new AllocationInput(
                1000,
                100,
                List.of(
                        area(1L, "Career", 1, 100, 1000),
                        area(2L, "Fitness", 2, 100, 1000),
                        area(3L, "Learning", 1, 100, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(250, result.allocations().get(0).recommendedMinutes());
        assertEquals(400, result.allocations().get(1).recommendedMinutes());
        assertEquals(250, result.allocations().get(2).recommendedMinutes());

        assertEquals(900, result.totalRecommendedMinutes());
    }

    @Test
    void shouldRespectMinimumMinutes() {

        AllocationInput input = new AllocationInput(
                1000,
                200,
                List.of(
                        area(1L, "Career", 1, 300, 1000),
                        area(2L, "Fitness", 1, 0, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(550, result.allocations().get(0).recommendedMinutes());
        assertEquals(250, result.allocations().get(1).recommendedMinutes());
        assertEquals(800, result.totalRecommendedMinutes());
    }

    @Test
    void shouldRespectMaximumMinutes() {

        AllocationInput input = new AllocationInput(
                1000,
                200,
                List.of(
                        area(1L, "Career", 1, 0, 300),
                        area(2L, "Fitness", 1, 0, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(300, result.allocations().get(0).recommendedMinutes());
        assertEquals(500, result.allocations().get(1).recommendedMinutes());
        assertEquals(800, result.totalRecommendedMinutes());
    }

    @Test
    void shouldRedistributeOverflowAfterAreaHitsMaximum() {

        AllocationInput input = new AllocationInput(
                1000,
                0,
                List.of(
                        area(1L, "Career", 1, 0, 100),
                        area(2L, "Fitness", 1, 0, 1000),
                        area(3L, "Learning", 1, 0, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(100, result.allocations().get(0).recommendedMinutes());
        assertEquals(450, result.allocations().get(1).recommendedMinutes());
        assertEquals(450, result.allocations().get(2).recommendedMinutes());

        assertEquals(1000, result.totalRecommendedMinutes());
    }

    @Test
    void shouldAllowZeroAvailableMinutesWhenMinimumsAreZero() {

        AllocationInput input = new AllocationInput(
                0,
                0,
                List.of(
                        area(1L, "Career", 1, 0, 100),
                        area(2L, "Fitness", 1, 0, 100)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(0, result.discretionaryMinutes());
        assertEquals(0, result.totalRecommendedMinutes());
        assertEquals(0, result.allocations().get(0).recommendedMinutes());
        assertEquals(0, result.allocations().get(1).recommendedMinutes());
    }

    @Test
    void shouldRejectInvalidWeight() {

        AllocationInput input = new AllocationInput(
                1000,
                0,
                List.of(
                        area(1L, "Career", 0, 0, 1000)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculate(input)
        );
    }

    @Test
    void shouldRejectNegativeMinimum() {

        AllocationInput input = new AllocationInput(
                1000,
                0,
                List.of(
                        area(1L, "Career", 1, -10, 1000)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculate(input)
        );
    }

    @Test
    void shouldRejectMaximumLessThanMinimum() {

        AllocationInput input = new AllocationInput(
                1000,
                0,
                List.of(
                        area(1L, "Career", 1, 500, 400)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculate(input)
        );
    }

    @Test
    void shouldRejectWhenMinimumsExceedDiscretionaryMinutes() {

        AllocationInput input = new AllocationInput(
                1000,
                800,
                List.of(
                        area(1L, "Career", 1, 300, 1000)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculate(input)
        );
    }

    @Test
    void shouldHandleProportionalDecimalResultUsingIntegerMinutes() {

        AllocationInput input = new AllocationInput(
                100,
                0,
                List.of(
                        area(1L, "Career", 1, 0, 100),
                        area(2L, "Fitness", 1, 0, 100),
                        area(3L, "Learning", 1, 0, 100)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(34, result.allocations().get(0).recommendedMinutes());
        assertEquals(33, result.allocations().get(1).recommendedMinutes());
        assertEquals(33, result.allocations().get(2).recommendedMinutes());

        assertEquals(100, result.totalRecommendedMinutes());
    }

    @Test
    void shouldUseDeterministicRemainderDistribution() {

        AllocationInput input = new AllocationInput(
                10,
                0,
                List.of(
                        area(1L, "Career", 1, 0, 10),
                        area(2L, "Fitness", 1, 0, 10),
                        area(3L, "Learning", 1, 0, 10)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(4, result.allocations().get(0).recommendedMinutes());
        assertEquals(3, result.allocations().get(1).recommendedMinutes());
        assertEquals(3, result.allocations().get(2).recommendedMinutes());
    }

    @Test
    void shouldAlwaysMatchExactDiscretionaryTotal() {

        AllocationInput input = new AllocationInput(
                997,
                197,
                List.of(
                        area(1L, "Career", 2, 50, 1000),
                        area(2L, "Fitness", 3, 30, 1000),
                        area(3L, "Learning", 5, 20, 1000)
                )
        );

        AllocationResult result = engine.calculate(input);

        assertEquals(800, result.discretionaryMinutes());
        assertEquals(800, result.totalRecommendedMinutes());

        int sum = result.allocations()
                .stream()
                .mapToInt(AllocationResultItem::recommendedMinutes)
                .sum();

        assertEquals(800, sum);
    }

    @Test
    void shouldProduceIdenticalResultForIdenticalInput() {

        AllocationInput input = new AllocationInput(
                1000,
                100,
                List.of(
                        area(1L, "Career", 3, 50, 800),
                        area(2L, "Fitness", 2, 20, 500),
                        area(3L, "Learning", 1, 0, 500)
                )
        );

        AllocationResult first = engine.calculate(input);
        AllocationResult second = engine.calculate(input);

        assertEquals(first, second);
    }

    @Test
    void shouldRejectWhenAllMaximumsCannotSatisfyExactTotal() {

        AllocationInput input = new AllocationInput(
                1000,
                0,
                List.of(
                        area(1L, "Career", 1, 0, 200),
                        area(2L, "Fitness", 1, 0, 200)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculate(input)
        );
    }

    private LifeAreaAllocationInput area(
            Long id,
            String name,
            int weight,
            int min,
            int max
    ) {
        return new LifeAreaAllocationInput(
                id,
                name,
                weight,
                min,
                max
        );
    }
}
