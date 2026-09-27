package com.weekahead.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import io.jsonwebtoken.JwtException;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.auth.service.JwtService;
import com.weekahead.config.SecurityConfig;

@WebMvcTest(AuthenticatedUserController.class)
@Import(SecurityConfig.class)
class AuthenticatedUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldReturn401WhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenInvalidJwtIsProvided() throws Exception {
        when(jwtService.extractEmail("invalid-token"))
                .thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(
                get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token")
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "tanmay@example.com")
    void shouldReturn200WhenUserIsAuthenticated() throws Exception {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        ReflectionTestUtils.setField(user, "id", 1L);

        when(currentUserService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }
}
