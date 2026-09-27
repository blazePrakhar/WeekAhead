package com.weekahead.lifearea.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.weekahead.auth.service.JwtService;
import com.weekahead.lifearea.dto.LifeAreaResponse;
import com.weekahead.lifearea.service.LifeAreaService;

@WebMvcTest(LifeAreaController.class)
class LifeAreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LifeAreaService lifeAreaService;

    @MockBean
    private JwtService jwtService;

    private LifeAreaResponse response() {
        return new LifeAreaResponse(
                1L,
                "Career",
                "Backend development",
                8,
                300,
                900,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void shouldCreateLifeArea() throws Exception {
        when(lifeAreaService.create(any()))
                .thenReturn(response());

        String request = """
                {
                    "name": "Career",
                    "description": "Backend development",
                    "weight": 8,
                    "minMinutes": 300,
                    "maxMinutes": 900
                }
                """;

        mockMvc.perform(
                post("/api/life-areas")
                        .with(csrf())
                        .with(user("tanmay@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidLifeAreaRequest() throws Exception {
        String request = """
                {
                    "name": "",
                    "description": "Backend development",
                    "weight": 0,
                    "minMinutes": -1,
                    "maxMinutes": 900
                }
                """;

        mockMvc.perform(
                post("/api/life-areas")
                        .with(csrf())
                        .with(user("tanmay@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetLifeAreas() throws Exception {
        when(lifeAreaService.getAll())
                .thenReturn(List.of(response()));

        mockMvc.perform(
                get("/api/life-areas")
                        .with(user("tanmay@example.com"))
        ).andExpect(status().isOk());
    }

    @Test
    void shouldUpdateLifeArea() throws Exception {
        when(lifeAreaService.update(any(Long.class), any()))
                .thenReturn(response());

        String request = """
                {
                    "name": "Career",
                    "description": "Java and Spring Boot",
                    "weight": 8,
                    "minMinutes": 300,
                    "maxMinutes": 900
                }
                """;

        mockMvc.perform(
                put("/api/life-areas/1")
                        .with(csrf())
                        .with(user("tanmay@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(status().isOk());
    }

    @Test
    void shouldArchiveLifeArea() throws Exception {
        mockMvc.perform(
                delete("/api/life-areas/1")
                        .with(csrf())
                        .with(user("tanmay@example.com"))
        ).andExpect(status().isNoContent());

        verify(lifeAreaService).delete(1L);
    }
}