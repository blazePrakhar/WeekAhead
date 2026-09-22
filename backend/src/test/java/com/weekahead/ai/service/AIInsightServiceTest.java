package com.weekahead.ai.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekahead.ai.client.LLMClient;
import com.weekahead.ai.dto.AIInsightResponse;
import com.weekahead.ai.dto.AIInsightSummary;
import com.weekahead.allocation.entity.WeeklyAllocation;
import com.weekahead.allocation.repository.WeeklyAllocationRepository;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.neglect.service.NeglectService;
import com.weekahead.rebalancing.service.RebalancingService;
import com.weekahead.timetracking.repository.TimeLogRepository;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@ExtendWith(MockitoExtension.class)
class AIInsightServiceTest {

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private WeeklyAllocationRepository weeklyAllocationRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private LifeAreaRepository lifeAreaRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NeglectService neglectService;

    @Mock
    private RebalancingService rebalancingService;

    @Mock
    private LLMClient llmClient;

    private ObjectMapper objectMapper;

    private AIInsightService aiInsightService;

    private User user;

    private Week week;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();

        aiInsightService
                = new AIInsightService(
                        weekRepository,
                        weeklyAllocationRepository,
                        timeLogRepository,
                        lifeAreaRepository,
                        currentUserService,
                        neglectService,
                        rebalancingService,
                        llmClient,
                        objectMapper
                );

        user
                = new User(
                        "test@example.com",
                        "hashed-password",
                        Role.USER,
                        UserStatus.ACTIVE
                );

        /*
         * Week uses a generated database ID and has no setId() method.
         * Mock it for this unit test.
         */
        week = mock(Week.class);

        lenient().when(week.getId())
                .thenReturn(1L);

        lenient().when(week.getWeekStartDate())
                .thenReturn(LocalDate.of(2026, 9, 21));

        lenient().when(week.getWeekEndDate())
                .thenReturn(LocalDate.of(2026, 9, 27));

        lenient().when(week.getAvailableMinutes())
                .thenReturn(2400);

        lenient().when(week.getFixedCommitmentMinutes())
                .thenReturn(600);
    }

    @Test
    void buildSummary_shouldBuildControlledSummary() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(
                weekRepository.findByIdAndUserId(
                        1L,
                        user.getId()
                )
        ).thenReturn(
                Optional.of(week)
        );

        LifeArea lifeArea = mock(LifeArea.class);

        when(lifeArea.getId())
                .thenReturn(1L);

        when(lifeArea.getName())
                .thenReturn("Study");

        WeeklyAllocation allocation
                = new WeeklyAllocation(
                        week,
                        lifeArea,
                        300,
                        250,
                        180,
                        "v1",
                        null
                );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                week.getId()
                        )
        ).thenReturn(
                List.of(allocation)
        );

        when(
                timeLogRepository.sumDurationByLifeArea(
                        user.getId(),
                        week.getWeekStartDate(),
                        week.getWeekEndDate()
                )
        ).thenReturn(
                List.<Object[]>of(
                        new Object[]{1L, 180L}
                )
        );

        when(
                lifeAreaRepository.findAllByUserId(
                        user.getId()
                )
        ).thenReturn(
                List.of(lifeArea)
        );

        when(
                neglectService.calculate()
        ).thenReturn(
                List.of()
        );

        when(
                rebalancingService.calculate(1L)
        ).thenReturn(
                List.of()
        );

        AIInsightSummary summary
                = aiInsightService.buildSummary(1L);

        assertNotNull(summary);

        assertEquals(
                1L,
                summary.weekId()
        );

        assertEquals(
                "2026-09-21",
                summary.weekStartDate()
        );

        assertEquals(
                "2026-09-27",
                summary.weekEndDate()
        );

        assertEquals(
                2400,
                summary.availableMinutes()
        );

        assertEquals(
                600,
                summary.fixedCommitmentMinutes()
        );

        assertEquals(
                1,
                summary.lifeAreas().size()
        );

        assertEquals(
                1L,
                summary.lifeAreas()
                        .get(0)
                        .lifeAreaId()
        );

        assertEquals(
                "Study",
                summary.lifeAreas()
                        .get(0)
                        .lifeAreaName()
        );

        assertEquals(
                300,
                summary.lifeAreas()
                        .get(0)
                        .recommendedMinutes()
        );

        assertEquals(
                250,
                summary.lifeAreas()
                        .get(0)
                        .plannedMinutes()
        );

        assertEquals(
                180,
                summary.lifeAreas()
                        .get(0)
                        .actualMinutes()
        );

        assertEquals(
                0.6,
                summary.lifeAreas()
                        .get(0)
                        .utilization()
        );

        assertEquals(
                "NORMAL",
                summary.lifeAreas()
                        .get(0)
                        .neglectLevel()
        );

        assertEquals(
                0,
                summary.lifeAreas()
                        .get(0)
                        .consecutiveUnderTargetWeeks()
        );

        assertEquals(
                0,
                summary.rebalancingSuggestions().size()
        );
    }

    @Test
    void buildSummary_shouldUseZeroActualMinutesWhenNoLogsExist() {

        LifeArea lifeArea = mock(LifeArea.class);

        when(lifeArea.getId())
                .thenReturn(1L);

        when(lifeArea.getName())
                .thenReturn("Study");

        WeeklyAllocation allocation
                = new WeeklyAllocation(
                        week,
                        lifeArea,
                        300,
                        300,
                        0,
                        "v1",
                        null
                );

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(
                weekRepository.findByIdAndUserId(
                        1L,
                        user.getId()
                )
        ).thenReturn(
                Optional.of(week)
        );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                week.getId()
                        )
        ).thenReturn(
                List.of(allocation)
        );

        when(
                timeLogRepository.sumDurationByLifeArea(
                        user.getId(),
                        week.getWeekStartDate(),
                        week.getWeekEndDate()
                )
        ).thenReturn(
                List.of()
        );

        when(
                lifeAreaRepository.findAllByUserId(
                        user.getId()
                )
        ).thenReturn(
                List.of(lifeArea)
        );

        when(
                neglectService.calculate()
        ).thenReturn(
                List.of()
        );

        when(
                rebalancingService.calculate(1L)
        ).thenReturn(
                List.of()
        );

        AIInsightSummary summary
                = aiInsightService.buildSummary(1L);

        assertEquals(
                1,
                summary.lifeAreas().size()
        );

        assertEquals(
                0,
                summary.lifeAreas()
                        .get(0)
                        .actualMinutes()
        );

        assertEquals(
                0.0,
                summary.lifeAreas()
                        .get(0)
                        .utilization()
        );
    }

    @Test
    void buildSummary_shouldRejectWeekBelongingToAnotherUser() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(
                weekRepository.findByIdAndUserId(
                        99L,
                        user.getId()
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> aiInsightService.buildSummary(99L)
        );
    }

    @Test
    void buildSummary_shouldReturnEmptyCollectionsWhenNoAllocationsExist() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(
                weekRepository.findByIdAndUserId(
                        1L,
                        user.getId()
                )
        ).thenReturn(
                Optional.of(week)
        );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                week.getId()
                        )
        ).thenReturn(
                List.of()
        );

        when(
                timeLogRepository.sumDurationByLifeArea(
                        user.getId(),
                        week.getWeekStartDate(),
                        week.getWeekEndDate()
                )
        ).thenReturn(
                List.of()
        );

        when(
                lifeAreaRepository.findAllByUserId(
                        user.getId()
                )
        ).thenReturn(
                List.of()
        );

        when(
                neglectService.calculate()
        ).thenReturn(
                List.of()
        );

        when(
                rebalancingService.calculate(1L)
        ).thenReturn(
                List.of()
        );

        AIInsightSummary summary
                = aiInsightService.buildSummary(1L);

        assertNotNull(summary);

        assertEquals(
                List.of(),
                summary.lifeAreas()
        );

        assertEquals(
                List.of(),
                summary.rebalancingSuggestions()
        );
    }

    @Test
    void generateInsight_shouldSendControlledSummaryToLLM() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(
                weekRepository.findByIdAndUserId(
                        1L,
                        user.getId()
                )
        ).thenReturn(
                Optional.of(week)
        );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                week.getId()
                        )
        ).thenReturn(
                List.of()
        );

        when(
                timeLogRepository.sumDurationByLifeArea(
                        user.getId(),
                        week.getWeekStartDate(),
                        week.getWeekEndDate()
                )
        ).thenReturn(
                List.of()
        );

        when(
                lifeAreaRepository.findAllByUserId(
                        user.getId()
                )
        ).thenReturn(
                List.of()
        );

        when(
                neglectService.calculate()
        ).thenReturn(
                List.of()
        );

        when(
                rebalancingService.calculate(1L)
        ).thenReturn(
                List.of()
        );

        String llmResponse
                = """
                {
                  "summary": "Your week was balanced.",
                  "observations": [
                    "No major issues were detected."
                  ],
                  "actions": [
                    "Continue the current routine."
                  ]
                }
                """;

        when(
                llmClient.generateInsight(
                        org.mockito.ArgumentMatchers.anyString()
                )
        ).thenReturn(
                llmResponse
        );

        AIInsightResponse response
                = aiInsightService.generateInsight(1L);

        assertNotNull(response);

        assertEquals(
                1L,
                response.weekId()
        );

        assertEquals(
                llmResponse,
                response.insight()
        );
    }

    @Test
    void generateInsight_shouldRejectInvalidLLMResponse() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(
                weekRepository.findByIdAndUserId(
                        1L,
                        user.getId()
                )
        ).thenReturn(
                Optional.of(week)
        );

        when(
                weeklyAllocationRepository
                        .findAllByWeekIdOrderByLifeAreaIdAsc(
                                week.getId()
                        )
        ).thenReturn(
                List.of()
        );

        when(
                timeLogRepository.sumDurationByLifeArea(
                        user.getId(),
                        week.getWeekStartDate(),
                        week.getWeekEndDate()
                )
        ).thenReturn(
                List.of()
        );

        when(
                lifeAreaRepository.findAllByUserId(
                        user.getId()
                )
        ).thenReturn(
                List.of()
        );

        when(
                neglectService.calculate()
        ).thenReturn(
                List.of()
        );

        when(
                rebalancingService.calculate(1L)
        ).thenReturn(
                List.of()
        );

        when(
                llmClient.generateInsight(
                        org.mockito.ArgumentMatchers.anyString()
                )
        ).thenReturn(
                """
                {
                  "summary": "",
                  "observations": [],
                  "actions": []
                }
                """
        );

        assertThrows(
                IllegalStateException.class,
                () -> aiInsightService.generateInsight(1L)
        );
    }
}
