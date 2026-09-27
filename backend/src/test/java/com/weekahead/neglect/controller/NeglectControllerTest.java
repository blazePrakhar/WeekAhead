package com.weekahead.neglect.controller;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.weekahead.auth.service.JwtService;
import com.weekahead.config.SecurityConfig;
import com.weekahead.neglect.model.NeglectAssessment;
import com.weekahead.neglect.model.NeglectLevel;
import com.weekahead.neglect.service.NeglectService;

@WebMvcTest(NeglectController.class)
@Import(SecurityConfig.class)
class NeglectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NeglectService neglectService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldReturnNeglectAssessments() throws Exception {

        NeglectAssessment assessment =
                new NeglectAssessment(
                        1L,
                        0.40,
                        2,
                        NeglectLevel.WARNING
                );

        when(neglectService.calculate())
                .thenReturn(List.of(assessment));

        mockMvc.perform(
                get("/api/neglect")
                        .with(user("test@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lifeAreaId").value(1))
                .andExpect(jsonPath("$[0].utilization").value(0.40))
                .andExpect(jsonPath("$[0].consecutiveUnderTargetWeeks").value(2))
                .andExpect(jsonPath("$[0].level").value("WARNING"));
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoNeglectAssessments()
            throws Exception {

        when(neglectService.calculate())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/neglect")
                        .with(user("test@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn401WhenGettingNeglectWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                get("/api/neglect")
        )
                .andExpect(status().isUnauthorized());
    }
}