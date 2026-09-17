package com.weekahead.auth.service;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.repository.UserRepository;

class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private AutoCloseable mocks;

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();

        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void shouldReturnCurrentUser() {
        mocks = MockitoAnnotations.openMocks(this);

        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "tanmay@example.com",
                        null,
                        java.util.Collections.emptyList()
                )
        );

        when(userRepository.findByEmail("tanmay@example.com"))
                .thenReturn(Optional.of(user));

        CurrentUserService currentUserService
                = new CurrentUserService(userRepository);

        User result = currentUserService.getCurrentUser();

        assertEquals("tanmay@example.com", result.getEmail());
    }

    @Test
    void shouldRejectUnauthenticatedUser() {
        mocks = MockitoAnnotations.openMocks(this);

        SecurityContextHolder.clearContext();

        CurrentUserService currentUserService
                = new CurrentUserService(userRepository);

        assertThrows(
                IllegalStateException.class,
                currentUserService::getCurrentUser
        );
    }
}
