package com.weekahead.allocation.controller;

import com.weekahead.allocation.dto.AllocationResponse;
import com.weekahead.allocation.service.AllocationService;
import com.weekahead.allocation.service.AllocationSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weeks")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/{id}/recommendation/generate")
    public ResponseEntity<AllocationResponse> generateRecommendation(
            @PathVariable Long id
    ) {
        AllocationSnapshot snapshot =
                allocationService.generateRecommendation(id);

        return ResponseEntity.ok(toResponse(snapshot));
    }

    @GetMapping("/{id}/allocations")
    public ResponseEntity<AllocationResponse> getAllocations(
            @PathVariable Long id
    ) {
        AllocationSnapshot snapshot =
                allocationService.getAllocations(id);

        return ResponseEntity.ok(toResponse(snapshot));
    }

    private AllocationResponse toResponse(AllocationSnapshot snapshot) {

        List<AllocationResponse.AllocationItem> allocations =
                snapshot.result().allocations()
                        .stream()
                        .map(item -> new AllocationResponse.AllocationItem(
                                item.lifeAreaId(),
                                item.name(),
                                item.weight(),
                                item.recommendedMinutes(),
                                item.minMinutes(),
                                item.maxMinutes()
                        ))
                        .toList();

        return new AllocationResponse(
                snapshot.week().getId(),
                snapshot.week().getWeekStartDate(),
                snapshot.week().getWeekEndDate(),
                snapshot.week().getAvailableMinutes(),
                snapshot.week().getFixedCommitmentMinutes(),
                snapshot.result().discretionaryMinutes(),
                snapshot.result().totalRecommendedMinutes(),
                allocations
        );
    }
}