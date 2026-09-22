package com.weekahead.week.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.week.dto.WeekRequest;
import com.weekahead.week.dto.WeekResponse;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@ExtendWith(MockitoExtension.class)
class WeekServiceTest {

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private CurrentUserService currentUserService;

    private WeekService weekService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        weekService = new WeekService(
                weekRepository,
                currentUserService
        );

        currentUser = new User(
                "test@example.com",
                "hashed-password",
                Role.USER,
                UserStatus.ACTIVE
        );
    }

    @Test
    void shouldCreateWeekSuccessfully() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);

        WeekRequest request = new WeekRequest(
                startDate,
                10080,
                7200
        );

        Week savedWeek = new Week(
                currentUser,
                startDate,
                startDate.plusDays(6),
                10080,
                7200,
                null
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository.findByUserIdAndWeekStartDate(
                currentUser.getId(),
                startDate
        )).thenReturn(Optional.empty());

        when(weekRepository.save(org.mockito.ArgumentMatchers.any(Week.class)))
                .thenReturn(savedWeek);

        WeekResponse response = weekService.create(request);

        assertEquals(startDate, response.weekStartDate());
        assertEquals(startDate.plusDays(6), response.weekEndDate());
        assertEquals(10080, response.availableMinutes());
        assertEquals(7200, response.fixedCommitmentMinutes());
    }

    @Test
    void shouldRejectFixedCommitmentGreaterThanAvailableMinutes() {
        WeekRequest request = new WeekRequest(
                LocalDate.of(2026, 9, 21),
                5000,
                6000
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> weekService.create(request)
        );

        assertEquals(
                "Fixed commitment minutes must be less than or equal to available minutes",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDuplicateWeek() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);

        WeekRequest request = new WeekRequest(
                startDate,
                10080,
                7200
        );

        Week existingWeek = new Week(
                currentUser,
                startDate,
                startDate.plusDays(6),
                10080,
                7200,
                null
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository.findByUserIdAndWeekStartDate(
                currentUser.getId(),
                startDate
        )).thenReturn(Optional.of(existingWeek));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> weekService.create(request)
        );

        assertEquals(
                "A week already exists for the given start date",
                exception.getMessage()
        );
    }

    @Test
    void shouldGetCurrentWeek() {
        LocalDate today = LocalDate.of(2026, 9, 24);
        LocalDate startDate = LocalDate.of(2026, 9, 21);
        LocalDate endDate = LocalDate.of(2026, 9, 27);

        Week week = new Week(
                currentUser,
                startDate,
                endDate,
                10080,
                7200,
                null
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        currentUser.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.of(week));

        WeekResponse response = weekService.getCurrent();

        assertEquals(startDate, response.weekStartDate());
        assertEquals(endDate, response.weekEndDate());
        assertEquals(10080, response.availableMinutes());
        assertEquals(7200, response.fixedCommitmentMinutes());
    }

    @Test
    void shouldRejectWhenCurrentWeekDoesNotExist() {
        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        currentUser.getId(),
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> weekService.getCurrent()
        );

        assertEquals(
                "Current week not found",
                exception.getMessage()
        );
    }

    @Test
    void shouldGetWeekByIdForCurrentUser() {
        Long weekId = 1L;
        LocalDate startDate = LocalDate.of(2026, 9, 21);

        Week week = new Week(
                currentUser,
                startDate,
                startDate.plusDays(6),
                10080,
                7200,
                null
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository.findByIdAndUserId(
                weekId,
                currentUser.getId()
        )).thenReturn(Optional.of(week));

        WeekResponse response = weekService.getById(weekId);

        assertEquals(startDate, response.weekStartDate());
        assertEquals(startDate.plusDays(6), response.weekEndDate());
    }

    @Test
    void shouldRejectAccessToAnotherUsersWeek() {
        Long weekId = 99L;

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository.findByIdAndUserId(
                weekId,
                currentUser.getId()
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> weekService.getById(weekId)
        );

        assertEquals(
                "Week not found",
                exception.getMessage()
        );
    }

    @Test
    void shouldGetLatestFourCompletedWeeks() {
        List<Week> weeks = List.of(
                new Week(
                        currentUser,
                        LocalDate.of(2026, 9, 14),
                        LocalDate.of(2026, 9, 20),
                        10080,
                        7200,
                        null
                ),
                new Week(
                        currentUser,
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2026, 9, 13),
                        10080,
                        7200,
                        null
                )
        );

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);

        when(weekRepository
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        org.mockito.ArgumentMatchers.eq(currentUser.getId()),
                        org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                        org.mockito.ArgumentMatchers.any()
                ))
                .thenReturn(weeks);

        List<Week> result = weekService.getLatestCompletedWeeks();

        assertEquals(2, result.size());
        assertEquals(weeks, result);

        verify(weekRepository)
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        org.mockito.ArgumentMatchers.eq(currentUser.getId()),
                        org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                        org.mockito.ArgumentMatchers.any()
                );
    }
}
