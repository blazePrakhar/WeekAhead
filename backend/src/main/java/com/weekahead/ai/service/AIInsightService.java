package com.weekahead.ai.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekahead.ai.client.LLMClient;
import com.weekahead.ai.dto.AIInsightContent;
import com.weekahead.ai.dto.AIInsightResponse;
import com.weekahead.ai.dto.AIInsightSummary;
import com.weekahead.ai.dto.AILifeAreaSummary;
import com.weekahead.ai.dto.AIRebalancingSummary;
import com.weekahead.ai.exception.AIProviderException;
import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.neglect.model.NeglectAssessment;
import com.weekahead.neglect.service.NeglectService;
import com.weekahead.rebalancing.model.RebalancingSuggestion;
import com.weekahead.rebalancing.service.RebalancingService;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@Service
public class AIInsightService {

    private final WeekRepository weekRepository;
    private final WeeklyAllocationRepository weeklyAllocationRepository;
    private final TimeLogRepository timeLogRepository;
    private final LifeAreaRepository lifeAreaRepository;
    private final CurrentUserService currentUserService;
    private final NeglectService neglectService;
    private final RebalancingService rebalancingService;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    public AIInsightService(
            WeekRepository weekRepository,
            WeeklyAllocationRepository weeklyAllocationRepository,
            TimeLogRepository timeLogRepository,
            LifeAreaRepository lifeAreaRepository,
            CurrentUserService currentUserService,
            NeglectService neglectService,
            RebalancingService rebalancingService,
            LLMClient llmClient,
            ObjectMapper objectMapper
    ) {
        this.weekRepository = weekRepository;
        this.weeklyAllocationRepository = weeklyAllocationRepository;
        this.timeLogRepository = timeLogRepository;
        this.lifeAreaRepository = lifeAreaRepository;
        this.currentUserService = currentUserService;
        this.neglectService = neglectService;
        this.rebalancingService = rebalancingService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public AIInsightResponse generateInsight(Long weekId) {

        AIInsightSummary summary = buildSummary(weekId);

        try {
            String structuredSummary =
                    objectMapper.writeValueAsString(summary);

            String llmResponse =
                    llmClient.generateInsight(structuredSummary);

            AIInsightContent content =
                    objectMapper.readValue(
                            llmResponse,
                            AIInsightContent.class
                    );

            validateInsight(content);

            return new AIInsightResponse(
                    weekId,
                    llmResponse
            );

        } catch (AIProviderException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate AI weekly insight",
                    exception
            );
        }
    }

    public AIInsightSummary buildSummary(Long weekId) {

        User currentUser =
                currentUserService.getCurrentUser();

        Week week =
                weekRepository
                        .findByIdAndUserId(
                                weekId,
                                currentUser.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Week not found"
                                )
                        );

        List<WeeklyAllocation> allocations =
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                week.getId()
                        );

        Map<Long, Integer> actualMinutesByLifeArea =
                buildActualMinutesMap(
                        timeLogRepository.sumDurationByLifeArea(
                                currentUser.getId(),
                                week.getWeekStartDate(),
                                week.getWeekEndDate()
                        )
                );

        Map<Long, LifeArea> lifeAreasById =
                buildLifeAreaMap(currentUser.getId());

        Map<Long, NeglectAssessment> neglectByLifeArea =
                buildNeglectMap();

        List<AILifeAreaSummary> lifeAreas =
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

                            Long lifeAreaId =
                                    lifeArea.getId();

                            int recommendedMinutes =
                                    allocation.getRecommendedMinutes();

                            int plannedMinutes =
                                    allocation.getPlannedMinutes();

                            int actualMinutes =
                                    actualMinutesByLifeArea
                                            .getOrDefault(
                                                    lifeAreaId,
                                                    0
                                            );

                            double utilization =
                                    calculateUtilization(
                                            actualMinutes,
                                            recommendedMinutes
                                    );

                            NeglectAssessment neglect =
                                    neglectByLifeArea.get(
                                            lifeAreaId
                                    );

                            String neglectLevel =
                                    neglect == null
                                            ? "NORMAL"
                                            : neglect.level().name();

                            int consecutiveUnderTargetWeeks =
                                    neglect == null
                                            ? 0
                                            : neglect
                                                    .consecutiveUnderTargetWeeks();

                            return new AILifeAreaSummary(
                                    lifeAreaId,
                                    lifeArea.getName(),
                                    recommendedMinutes,
                                    plannedMinutes,
                                    actualMinutes,
                                    utilization,
                                    neglectLevel,
                                    consecutiveUnderTargetWeeks
                            );
                        })
                        .toList();

        List<AIRebalancingSummary> rebalancingSuggestions =
                rebalancingService
                        .calculate(weekId)
                        .stream()
                        .map(this::toRebalancingSummary)
                        .toList();

        return new AIInsightSummary(
                week.getId(),
                week.getWeekStartDate().toString(),
                week.getWeekEndDate().toString(),
                week.getAvailableMinutes(),
                week.getFixedCommitmentMinutes(),
                lifeAreas,
                rebalancingSuggestions
        );
    }

    private void validateInsight(
            AIInsightContent content
    ) {

        if (content == null) {
            throw new IllegalStateException(
                    "LLM returned an empty insight"
            );
        }

        if (content.summary() == null
                || content.summary().isBlank()) {

            throw new IllegalStateException(
                    "LLM returned an empty summary"
            );
        }

        if (content.observations() == null) {
            throw new IllegalStateException(
                    "LLM returned null observations"
            );
        }

        if (content.actions() == null) {
            throw new IllegalStateException(
                    "LLM returned null actions"
            );
        }
    }

    private double calculateUtilization(
            int actualMinutes,
            int recommendedMinutes
    ) {

        if (recommendedMinutes <= 0) {
            return 0.0;
        }

        return (double) actualMinutes / recommendedMinutes;
    }

    private Map<Long, Integer> buildActualMinutesMap(
            List<Object[]> rows
    ) {

        Map<Long, Integer> result =
                new HashMap<>();

        for (Object[] row : rows) {

            Long lifeAreaId =
                    ((Number) row[0]).longValue();

            Integer totalMinutes =
                    ((Number) row[1]).intValue();

            result.put(
                    lifeAreaId,
                    totalMinutes
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

    private Map<Long, NeglectAssessment> buildNeglectMap() {

        Map<Long, NeglectAssessment> result =
                new HashMap<>();

        for (NeglectAssessment assessment :
                neglectService.calculate()) {

            result.put(
                    assessment.lifeAreaId(),
                    assessment
            );
        }

        return result;
    }

    private AIRebalancingSummary toRebalancingSummary(
            RebalancingSuggestion suggestion
    ) {

        return new AIRebalancingSummary(
                suggestion.sourceLifeAreaId(),
                suggestion.destinationLifeAreaId(),
                suggestion.transferableMinutes(),
                suggestion.explanation()
        );
    }
}