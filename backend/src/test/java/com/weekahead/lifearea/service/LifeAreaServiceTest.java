package com.weekahead.lifearea.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.dto.LifeAreaRequest;
import com.weekahead.lifearea.dto.LifeAreaResponse;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;

class LifeAreaServiceTest {

    private final LifeAreaRepository lifeAreaRepository
            = mock(LifeAreaRepository.class);

    private final CurrentUserService currentUserService
            = mock(CurrentUserService.class);

    private final LifeAreaService lifeAreaService
            = new LifeAreaService(lifeAreaRepository, currentUserService);

    @Test
    void shouldCreateLifeAreaForCurrentUser() {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        LifeArea lifeArea = new LifeArea(
                user,
                "Career",
                "Backend development",
                8,
                300,
                900
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(lifeAreaRepository.save(any(LifeArea.class))).thenReturn(lifeArea);

        LifeAreaResponse response = lifeAreaService.create(
                new LifeAreaRequest(
                        "Career",
                        "Backend development",
                        8,
                        300,
                        900
                )
        );

        assertEquals("Career", response.name());
        assertEquals("Backend development", response.description());
        assertEquals(8, response.weight());
        assertEquals(300, response.minMinutes());
        assertEquals(900, response.maxMinutes());
        assertEquals(true, response.isActive());

        verify(lifeAreaRepository).save(any(LifeArea.class));
    }

    @Test
    void shouldReturnOnlyCurrentUsersLifeAreas() {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);

        LifeArea career = new LifeArea(
                user,
                "Career",
                "Backend development",
                8,
                300,
                900
        );

        LifeArea fitness = new LifeArea(
                user,
                "Fitness",
                "Gym and football",
                6,
                180,
                600
        );

        when(lifeAreaRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of(career, fitness));

        List<LifeAreaResponse> result = lifeAreaService.getAll();

        assertEquals(2, result.size());

        assertEquals("Career", result.get(0).name());
        assertEquals(8, result.get(0).weight());
        assertEquals(300, result.get(0).minMinutes());
        assertEquals(900, result.get(0).maxMinutes());

        assertEquals("Fitness", result.get(1).name());
        assertEquals(6, result.get(1).weight());
        assertEquals(180, result.get(1).minMinutes());
        assertEquals(600, result.get(1).maxMinutes());

        verify(lifeAreaRepository).findAllByUserId(user.getId());
    }

    @Test
    void shouldUpdateLifeAreaConfiguration() {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        LifeArea lifeArea = new LifeArea(
                user,
                "Career",
                "Backend development",
                5,
                200,
                600
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);

        when(lifeAreaRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(lifeArea));

        when(lifeAreaRepository.save(any(LifeArea.class)))
                .thenReturn(lifeArea);

        LifeAreaResponse response = lifeAreaService.update(
                1L,
                new LifeAreaRequest(
                        "Career",
                        "Java and Spring Boot",
                        8,
                        300,
                        900
                )
        );

        assertEquals("Career", response.name());
        assertEquals("Java and Spring Boot", response.description());
        assertEquals(8, response.weight());
        assertEquals(300, response.minMinutes());
        assertEquals(900, response.maxMinutes());

        verify(lifeAreaRepository).save(lifeArea);
    }

    @Test
    void shouldRejectWhenMaximumMinutesAreLessThanMinimumMinutesOnCreate() {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);

        assertThrows(
                IllegalArgumentException.class,
                () -> lifeAreaService.create(
                        new LifeAreaRequest(
                                "Career",
                                "Backend development",
                                8,
                                900,
                                300
                        )
                )
        );

        verify(lifeAreaRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenMaximumMinutesAreLessThanMinimumMinutesOnUpdate() {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);

        assertThrows(
                IllegalArgumentException.class,
                () -> lifeAreaService.update(
                        1L,
                        new LifeAreaRequest(
                                "Career",
                                "Backend development",
                                8,
                                900,
                                300
                        )
                )
        );

        verify(lifeAreaRepository, never())
                .findByIdAndUserId(any(), any());
    }

    @Test
    void shouldNotUpdateLifeAreaOwnedByAnotherUser() {
        User currentUser = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        when(lifeAreaRepository.findByIdAndUserId(99L, currentUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> lifeAreaService.update(
                        99L,
                        new LifeAreaRequest(
                                "Hacked",
                                "Not yours",
                                8,
                                300,
                                900
                        )
                )
        );

        verify(lifeAreaRepository)
                .findByIdAndUserId(99L, currentUser.getId());

        verify(lifeAreaRepository, never()).save(any());
    }

    @Test
    void shouldNotDeleteLifeAreaOwnedByAnotherUser() {
        User currentUser = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        when(lifeAreaRepository.findByIdAndUserId(99L, currentUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> lifeAreaService.delete(99L)
        );

        verify(lifeAreaRepository)
                .findByIdAndUserId(99L, currentUser.getId());

        verify(lifeAreaRepository, never()).save(any());
        verify(lifeAreaRepository, never()).delete(any());
    }

    @Test
    void shouldArchiveLifeArea() {
        User user = new User(
                "tanmay@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );

        LifeArea lifeArea = new LifeArea(
                user,
                "Career",
                "Backend development",
                8,
                300,
                900
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);

        when(lifeAreaRepository.findByIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(lifeArea));

        when(lifeAreaRepository.save(any(LifeArea.class)))
                .thenReturn(lifeArea);

        lifeAreaService.delete(1L);

        assertEquals(false, lifeArea.getIsActive());

        verify(lifeAreaRepository).findByIdAndUserId(1L, user.getId());
        verify(lifeAreaRepository).save(lifeArea);
        verify(lifeAreaRepository, never()).delete(any());
    }
}
