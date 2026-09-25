package com.weekahead.rebalancing.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.rebalancing.model.RebalancingSuggestion;
import com.weekahead.rebalancing.service.RebalancingService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/rebalancing")
@SecurityRequirement(name = "bearerAuth")
public class RebalancingController {

    private final RebalancingService rebalancingService;

    public RebalancingController(
            RebalancingService rebalancingService
    ) {
        this.rebalancingService = rebalancingService;
    }

    @GetMapping
    public List<RebalancingSuggestion> getRebalancingSuggestions() {
        return rebalancingService.calculate();
    }
}