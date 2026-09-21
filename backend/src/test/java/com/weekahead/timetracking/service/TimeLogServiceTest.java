package com.weekahead.timetracking.service;

import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.timetracking.dto.CreateTimeLogRequest;
import com.weekahead.timetracking.dto.TimeLogResponse;
import com.weekahead.timetracking.dto.UpdateTimeLogRequest;
import com.weekahead.timetracking.entity.TimeLog;
import com.weekahead.timetracking.repository.TimeLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeLogServiceTest {

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private LifeAreaRepository lifeAreaRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private User user;

    @Mock
    private LifeArea lifeArea;

    @InjectMocks
    private TimeLogService timeLogService;

    @Test
    void shouldCreateTimeLogForCurrentUser() {
        when(user.getId()).thenReturn(1L);
        when(lifeArea.getId()).thenReturn(1L);
        when(lifeArea.getName()).thenReturn("Work");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(lifeAreaRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(lifeArea));

        CreateTimeLogRequest request = new CreateTimeLogRequest(
                1L,
                LocalDate.of(2026, 9, 21),
                90,
                "Backend development",
                "MANUAL"
        );

        TimeLog savedTimeLog = mock(TimeLog.class);

        when(savedTimeLog.getId()).thenReturn(1L);
        when(savedTimeLog.getLifeArea()).thenReturn(lifeArea);
        when(savedTimeLog.getLogDate()).thenReturn(request.logDate());
        when(savedTimeLog.getDurationMinutes()).thenReturn(request.durationMinutes());
        when(savedTimeLog.getNote()).thenReturn(request.note());
        when(savedTimeLog.getSource()).thenReturn(request.source());

        when(timeLogRepository.save(any(TimeLog.class)))
                .thenReturn(savedTimeLog);

        TimeLogResponse response = timeLogService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(1L, response.lifeAreaId());
        assertEquals("Work", response.lifeAreaName());
        assertEquals(LocalDate.of(2026, 9, 21), response.logDate());
        assertEquals(90, response.durationMinutes());
        assertEquals("Backend development", response.note());
        assertEquals("MANUAL", response.source());

        verify(timeLogRepository).save(any(TimeLog.class));
    }

    @Test
    void shouldRejectTimeLogWhenLifeAreaDoesNotBelongToCurrentUser() {
        when(user.getId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(lifeAreaRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        CreateTimeLogRequest request = new CreateTimeLogRequest(
                99L,
                LocalDate.of(2026, 9, 21),
                90,
                "Backend development",
                "MANUAL"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeLogService.create(request)
        );

        assertEquals("Life area not found", exception.getMessage());

        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOwnTimeLog() {
        when(user.getId()).thenReturn(1L);
        when(lifeArea.getId()).thenReturn(1L);
        when(lifeArea.getName()).thenReturn("Work");
        when(currentUserService.getCurrentUser()).thenReturn(user);

        TimeLog timeLog = new TimeLog(
                user,
                lifeArea,
                LocalDate.of(2026, 9, 20),
                60,
                "Old note",
                "MANUAL"
        );

        when(timeLogRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(timeLog));

        when(lifeAreaRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(lifeArea));

        when(timeLogRepository.save(timeLog))
                .thenReturn(timeLog);

        UpdateTimeLogRequest request = new UpdateTimeLogRequest(
                1L,
                LocalDate.of(2026, 9, 21),
                120,
                "Updated note",
                "MANUAL"
        );

        TimeLogResponse response = timeLogService.update(1L, request);

        assertEquals(1L, response.lifeAreaId());
        assertEquals("Work", response.lifeAreaName());
        assertEquals(LocalDate.of(2026, 9, 21), response.logDate());
        assertEquals(120, response.durationMinutes());
        assertEquals("Updated note", response.note());
        assertEquals("MANUAL", response.source());

        verify(timeLogRepository).save(timeLog);
    }

    @Test
    void shouldRejectUpdateWhenTimeLogDoesNotBelongToCurrentUser() {
        when(user.getId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(timeLogRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        UpdateTimeLogRequest request = new UpdateTimeLogRequest(
                1L,
                LocalDate.of(2026, 9, 21),
                120,
                "Updated note",
                "MANUAL"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeLogService.update(99L, request)
        );

        assertEquals("Time log not found", exception.getMessage());

        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void shouldDeleteOwnTimeLog() {
        when(user.getId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);

        TimeLog timeLog = mock(TimeLog.class);

        when(timeLogRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(timeLog));

        timeLogService.delete(1L);

        verify(timeLogRepository).delete(timeLog);
    }

    @Test
    void shouldRejectDeleteWhenTimeLogDoesNotBelongToCurrentUser() {
        when(user.getId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(timeLogRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeLogService.delete(99L)
        );

        assertEquals("Time log not found", exception.getMessage());

        verify(timeLogRepository, never()).delete(any());
    }

    @Test
    void shouldListTimeLogsWithinDateRange() {
        when(user.getId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);

        TimeLog first = mock(TimeLog.class);
        TimeLog second = mock(TimeLog.class);

        when(first.getLifeArea()).thenReturn(lifeArea);
        when(first.getDurationMinutes()).thenReturn(90);

        when(second.getLifeArea()).thenReturn(lifeArea);
        when(second.getDurationMinutes()).thenReturn(60);

        when(timeLogRepository
                .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                        1L,
                        LocalDate.of(2026, 9, 20),
                        LocalDate.of(2026, 9, 21)
                ))
                .thenReturn(List.of(first, second));

        List<TimeLogResponse> responses = timeLogService.findAll(
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 21),
                null
        );

        assertEquals(2, responses.size());
        assertEquals(90, responses.get(0).durationMinutes());
        assertEquals(60, responses.get(1).durationMinutes());
    }

    @Test
    void shouldFilterTimeLogsByLifeArea() {
        when(user.getId()).thenReturn(1L);
        when(lifeArea.getId()).thenReturn(1L);
        when(lifeArea.getName()).thenReturn("Work");
        when(currentUserService.getCurrentUser()).thenReturn(user);

        when(lifeAreaRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(lifeArea));

        TimeLog timeLog = mock(TimeLog.class);

        when(timeLog.getId()).thenReturn(1L);
        when(timeLog.getLifeArea()).thenReturn(lifeArea);
        when(timeLog.getDurationMinutes()).thenReturn(90);

        when(timeLogRepository
                .findAllByUserIdAndLogDateBetweenAndLifeAreaIdOrderByLogDateDescIdDesc(
                        1L,
                        LocalDate.of(2026, 9, 20),
                        LocalDate.of(2026, 9, 21),
                        1L
                ))
                .thenReturn(List.of(timeLog));

        List<TimeLogResponse> responses = timeLogService.findAll(
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 21),
                1L
        );

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals(1L, responses.get(0).lifeAreaId());
        assertEquals("Work", responses.get(0).lifeAreaName());
        assertEquals(90, responses.get(0).durationMinutes());

        verify(lifeAreaRepository).findByIdAndUserId(1L, 1L);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeLogService.findAll(
                        LocalDate.of(2026, 9, 22),
                        LocalDate.of(2026, 9, 21),
                        null
                )
        );

        assertEquals("From date cannot be after to date", exception.getMessage());

        verifyNoInteractions(timeLogRepository);
    }
}
