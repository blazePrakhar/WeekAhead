package com.weekahead.ai.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.ai.dto.AIInsightResponse;
import com.weekahead.ai.service.AIInsightService;

@RestController
@RequestMapping("/api/weeks")
public class AIInsightController {

    private final AIInsightService aiInsightService;

    public AIInsightController(AIInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
    }

    @PostMapping("/{weekId}/ai-insight")
    public AIInsightResponse generateInsight(
            @PathVariable Long weekId
    ) {
        return aiInsightService.generateInsight(weekId);
    }
}