package com.weekahead.week.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekahead.auth.service.JwtService;
import com.weekahead.config.SecurityConfig;
import com.weekahead.week.dto.WeekRequest;
import com.weekahead.week.dto.WeekResponse;
import com.weekahead.week.service.WeekService;

@WebMvcTest(WeekController.class)
@Import(SecurityConfig.class)
class WeekControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeekService weekService;

    @MockBean
    private JwtService jwtService;

 
    @Test
    void shouldCreateWeek() throws Exception {
        WeekRequest request = new WeekRequest(
                LocalDate.of(2026, 9, 21),
                10080,
                7200
        );

        WeekResponse response = new WeekResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                10080,
                7200
        );

        when(weekService.create(any(WeekRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/weeks")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        "test@example.com"
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weekStartDate").value("2026-09-21"))
                .andExpect(jsonPath("$.weekEndDate").value("2026-09-27"))
                .andExpect(jsonPath("$.availableMinutes").value(10080))
                .andExpect(jsonPath("$.fixedCommitmentMinutes").value(7200));
    }

    @Test
    void shouldRejectNegativeAvailableMinutes() throws Exception {
        WeekRequest request = new WeekRequest(
                LocalDate.of(2026, 9, 21),
                -1,
                0
        );

        mockMvc.perform(post("/api/weeks")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        "test@example.com"
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeFixedCommitmentMinutes() throws Exception {
        WeekRequest request = new WeekRequest(
                LocalDate.of(2026, 9, 21),
                10080,
                -1
        );

        mockMvc.perform(post("/api/weeks")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        "test@example.com"
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingWeekStartDate() throws Exception {
        String request = """
                {
                    "availableMinutes": 10080,
                    "fixedCommitmentMinutes": 7200
                }
                """;

        mockMvc.perform(post("/api/weeks")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        "test@example.com"
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCurrentWeek() throws Exception {
        WeekResponse response = new WeekResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                10080,
                7200
        );

        when(weekService.getCurrent())
                .thenReturn(response);

        mockMvc.perform(get("/api/weeks/current")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        "test@example.com"
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weekStartDate").value("2026-09-21"))
                .andExpect(jsonPath("$.weekEndDate").value("2026-09-27"));
    }

    @Test
    void shouldGetWeekById() throws Exception {
        WeekResponse response = new WeekResponse(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 27),
                10080,
                7200
        );

        when(weekService.getById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/weeks/1")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        "test@example.com"
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.availableMinutes").value(10080));
    }

    @Test
    void shouldReturn401WhenCreatingWeekWithoutAuthentication() throws Exception {
        WeekRequest request = new WeekRequest(
                LocalDate.of(2026, 9, 21),
                10080,
                7200
        );

        mockMvc.perform(post("/api/weeks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenGettingCurrentWeekWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/weeks/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenGettingWeekByIdWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/weeks/1"))
                .andExpect(status().isUnauthorized());
    }
}
