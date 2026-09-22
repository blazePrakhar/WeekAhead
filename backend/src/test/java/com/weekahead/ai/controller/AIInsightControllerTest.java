package com.weekahead.ai.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.weekahead.ai.dto.AIInsightResponse;
import com.weekahead.ai.service.AIInsightService;
import com.weekahead.auth.service.JwtService;
import com.weekahead.config.SecurityConfig;

@WebMvcTest(AIInsightController.class)
@Import(SecurityConfig.class)
class AIInsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIInsightService aiInsightService;

    @MockBean
    private JwtService jwtService;

    @Test
    void generateInsight_shouldReturnInsightForAuthenticatedUser()
            throws Exception {

        String insight = """
                {
                  "summary": "Your week was balanced.",
                  "observations": [
                    "Study received consistent attention."
                  ],
                  "actions": [
                    "Continue the current routine."
                  ]
                }
                """;

        when(aiInsightService.generateInsight(10L))
                .thenReturn(
                        new AIInsightResponse(
                                10L,
                                insight
                        )
                );

        mockMvc.perform(
                        post("/api/weeks/10/ai-insight")
                                .with(user("test@example.com"))
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.weekId").value(10)
                )
                .andExpect(
                        jsonPath("$.insight").value(insight)
                );
    }

    @Test
    void generateInsight_shouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
                        post("/api/weeks/10/ai-insight")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateInsight_shouldPropagateServiceFailure()
            throws Exception {

        when(aiInsightService.generateInsight(10L))
                .thenThrow(
                        new IllegalArgumentException("Week not found")
                );

        mockMvc.perform(
                        post("/api/weeks/10/ai-insight")
                                .with(user("test@example.com"))
                )
                .andExpect(status().isBadRequest());
    }
}