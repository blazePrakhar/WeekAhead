package com.weekahead.auth.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weekahead.audit.service.AuditLogService;
import com.weekahead.auth.dto.LoginRequest;
import com.weekahead.auth.dto.LoginResponse;
import com.weekahead.auth.dto.RegisterRequest;
import com.weekahead.auth.dto.RegisterResponse;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.repository.UserRepository;

class AuthServiceTest {

    private UserRepository userRepository;
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuditLogService auditLogService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        passwordEncoder = org.mockito.Mockito.mock(
                org.springframework.security.crypto.password.PasswordEncoder.class
        );
        jwtService = org.mockito.Mockito.mock(JwtService.class);
        auditLogService = org.mockito.Mockito.mock(AuditLogService.class);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                auditLogService
        );
    }

    @Test
    void shouldRegisterUserAndCreateAuditLog() {
        RegisterRequest request = new RegisterRequest(
                " Test@Example.com ",
                "password123"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        User savedUser = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        org.springframework.test.util.ReflectionTestUtils.setField(
                savedUser,
                "id",
                1L
        );

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        RegisterResponse response =
                authService.register(request);

        assertEquals(
                "Registration successful",
                response.message()
        );

        verify(userRepository, times(1))
                .save(any(User.class));

        verify(auditLogService, times(1))
                .log(
                        savedUser,
                        "REGISTER",
                        "USER",
                        1L,
                        "User registration successful"
                );
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123"
        );

        User existingUser = new User(
                "test@example.com",
                "existing-hash",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(request)
                );

        assertEquals(
                "Email is already registered",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldLoginUserAndCreateAuditLog() {
        LoginRequest request = new LoginRequest(
                " Test@Example.com ",
                "password123"
        );

        User user = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        org.springframework.test.util.ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "hashed-password"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(
                1L,
                "test@example.com"
        )).thenReturn("access-token");

        LoginResponse response =
                authService.login(request);

        assertEquals(
                "Login successful",
                response.message()
        );

        assertEquals(
                "access-token",
                response.accessToken()
        );

        verify(jwtService, times(1))
                .generateAccessToken(
                        1L,
                        "test@example.com"
                );

        verify(auditLogService, times(1))
                .log(
                        user,
                        "LOGIN",
                        "USER",
                        1L,
                        "User login successful"
                );
    }

    @Test
    void shouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password"
        );

        User user = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "hashed-password"
        )).thenReturn(false);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateAccessToken(any(), any());

        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldRejectDisabledUser() {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        User user = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.DISABLED
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "hashed-password"
        )).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "User account is disabled",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateAccessToken(any(), any());

        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
}