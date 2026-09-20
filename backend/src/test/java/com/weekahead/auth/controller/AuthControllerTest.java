package com.weekahead.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.weekahead.auth.dto.LoginResponse;
import com.weekahead.auth.dto.RegisterResponse;
import com.weekahead.auth.service.AuthService;
import com.weekahead.auth.service.JwtService;
import com.weekahead.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRegisterUserWithValidRequest() throws Exception {
        when(authService.register(any()))
                .thenReturn(new RegisterResponse("Registration successful"));

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "tanmay@example.com",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                {
                    "message": "Registration successful"
                }
                """));
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "invalid-email",
                                "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "tanmay@example.com",
                                "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
            {
                "message": "Email is already registered"
            }
            """));
    }

    @Test
    void shouldLoginUserWithValidCredentials() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResponse(
                        "Login successful",
                        "test-access-token"
                ));

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "tanmay@example.com",
                                "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("""
            {
                "message": "Login successful",
                "accessToken": "test-access-token"
            }
            """));
    }

    @Test
    void shouldRejectInvalidLogin() throws Exception {
        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "tanmay@example.com",
                                "password": "wrongpassword"
                            }
                            """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
            {
                "message": "Invalid email or password"
            }
            """));
    }
}
