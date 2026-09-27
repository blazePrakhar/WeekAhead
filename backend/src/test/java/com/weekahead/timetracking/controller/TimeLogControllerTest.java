package com.weekahead.timetracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.auth.service.JwtService;
import com.weekahead.timetracking.dto.CreateTimeLogRequest;
import com.weekahead.timetracking.dto.TimeLogResponse;
import com.weekahead.timetracking.dto.UpdateTimeLogRequest;
import com.weekahead.timetracking.service.TimeLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimeLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeLogService timeLogService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldCreateTimeLog() throws Exception {
        CreateTimeLogRequest request = new CreateTimeLogRequest(
                1L,
                LocalDate.of(2026, 9, 21),
                90,
                "Backend development",
                "MANUAL"
        );

        TimeLogResponse response = new TimeLogResponse(
                1L,
                1L,
                "Work",
                LocalDate.of(2026, 9, 21),
                90,
                "Backend development",
                "MANUAL",
                LocalDateTime.of(2026, 9, 21, 7, 30)
        );

        when(timeLogService.create(any(CreateTimeLogRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lifeAreaId").value(1))
                .andExpect(jsonPath("$.lifeAreaName").value("Work"))
                .andExpect(jsonPath("$.logDate").value("2026-09-21"))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.note").value("Backend development"))
                .andExpect(jsonPath("$.source").value("MANUAL"));

        verify(timeLogService).create(any(CreateTimeLogRequest.class));
    }

    @Test
    void shouldUpdateTimeLog() throws Exception {
        UpdateTimeLogRequest request = new UpdateTimeLogRequest(
                1L,
                LocalDate.of(2026, 9, 21),
                120,
                "Updated note",
                "MANUAL"
        );

        TimeLogResponse response = new TimeLogResponse(
                1L,
                1L,
                "Work",
                LocalDate.of(2026, 9, 21),
                120,
                "Updated note",
                "MANUAL",
                LocalDateTime.of(2026, 9, 21, 7, 30)
        );

        when(timeLogService.update(
                eq(1L),
                any(UpdateTimeLogRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/time-logs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.durationMinutes").value(120))
                .andExpect(jsonPath("$.note").value("Updated note"));

        verify(timeLogService).update(
                eq(1L),
                any(UpdateTimeLogRequest.class)
        );
    }

    @Test
    void shouldDeleteTimeLog() throws Exception {
        doNothing().when(timeLogService).delete(1L);

        mockMvc.perform(delete("/api/time-logs/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(timeLogService).delete(1L);
    }

    @Test
    void shouldGetTimeLogs() throws Exception {
        TimeLogResponse response = new TimeLogResponse(
                1L,
                1L,
                "Work",
                LocalDate.of(2026, 9, 21),
                90,
                "Backend development",
                "MANUAL",
                LocalDateTime.of(2026, 9, 21, 7, 30)
        );

        when(timeLogService.findAll(
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 21),
                null
        )).thenReturn(List.of(response));

        mockMvc.perform(get("/api/time-logs")
                        .param("from", "2026-09-15")
                        .param("to", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].lifeAreaId").value(1))
                .andExpect(jsonPath("$[0].lifeAreaName").value("Work"))
                .andExpect(jsonPath("$[0].logDate").value("2026-09-21"))
                .andExpect(jsonPath("$[0].durationMinutes").value(90));

        verify(timeLogService).findAll(
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 21),
                null
        );
    }

    @Test
    void shouldGetTimeLogsFilteredByLifeArea() throws Exception {
        when(timeLogService.findAll(
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 21),
                1L
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/time-logs")
                        .param("from", "2026-09-15")
                        .param("to", "2026-09-21")
                        .param("lifeAreaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(timeLogService).findAll(
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 21),
                1L
        );
    }

    @Test
    void shouldRejectCreateWhenDurationIsInvalid() throws Exception {
        String request = """
                {
                  "lifeAreaId": 1,
                  "logDate": "2026-09-21",
                  "durationMinutes": 0,
                  "note": "Backend development",
                  "source": "MANUAL"
                }
                """;

        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeLogService);
    }

    @Test
    void shouldRejectCreateWhenRequiredFieldsAreMissing() throws Exception {
        String request = """
                {
                  "note": "Backend development"
                }
                """;

        mockMvc.perform(post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeLogService);
    }

    @Test
    void shouldRejectUpdateWhenDurationIsInvalid() throws Exception {
        String request = """
                {
                  "lifeAreaId": 1,
                  "logDate": "2026-09-21",
                  "durationMinutes": -10,
                  "note": "Invalid duration",
                  "source": "MANUAL"
                }
                """;

        mockMvc.perform(put("/api/time-logs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeLogService);
    }

    @Test
    void shouldRejectGetWhenFromDateIsMissing() throws Exception {
        mockMvc.perform(get("/api/time-logs")
                        .param("to", "2026-09-21"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeLogService);
    }

    @Test
    void shouldRejectGetWhenToDateIsMissing() throws Exception {
        mockMvc.perform(get("/api/time-logs")
                        .param("from", "2026-09-15"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeLogService);
    }

    @Test
    void shouldRejectGetWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/time-logs")
                        .param("from", "21-09-2026")
                        .param("to", "2026-09-21"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeLogService);
    }
}