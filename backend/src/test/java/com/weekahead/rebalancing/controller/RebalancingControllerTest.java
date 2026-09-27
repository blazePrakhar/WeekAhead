package com.weekahead.rebalancing.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.weekahead.auth.service.JwtService;
import com.weekahead.config.SecurityConfig;
import com.weekahead.rebalancing.model.RebalancingSuggestion;
import com.weekahead.rebalancing.service.RebalancingService;

@WebMvcTest(RebalancingController.class)
@Import(SecurityConfig.class)
class RebalancingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RebalancingService rebalancingService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldReturnRebalancingSuggestions()
            throws Exception {

        RebalancingSuggestion suggestion =
                new RebalancingSuggestion(
                        1L,
                        2L,
                        40,
                        "Transfer 40 minutes from Health to Learning."
                );

        when(rebalancingService.calculate())
                .thenReturn(List.of(suggestion));

        mockMvc.perform(
                get("/api/rebalancing")
                        .with(user("test@example.com"))
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].sourceLifeAreaId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].destinationLifeAreaId")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].transferableMinutes")
                                .value(40)
                )
                .andExpect(
                        jsonPath("$[0].explanation")
                                .value(
                                        "Transfer 40 minutes from Health to Learning."
                                )
                );
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoSuggestions()
            throws Exception {

        when(rebalancingService.calculate())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/rebalancing")
                        .with(user("test@example.com"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn401WhenGettingRebalancingWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                get("/api/rebalancing")
        )
                .andExpect(status().isUnauthorized());
    }
}