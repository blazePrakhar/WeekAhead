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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final LifeAreaRepository lifeAreaRepository;
    private final CurrentUserService currentUserService;

    public TimeLogService(
            TimeLogRepository timeLogRepository,
            LifeAreaRepository lifeAreaRepository,
            CurrentUserService currentUserService
    ) {
        this.timeLogRepository = timeLogRepository;
        this.lifeAreaRepository = lifeAreaRepository;
        this.currentUserService = currentUserService;
    }

    public TimeLogResponse create(CreateTimeLogRequest request) {
        User user = currentUserService.getCurrentUser();

        LifeArea lifeArea = getOwnedLifeArea(request.lifeAreaId(), user.getId());

        TimeLog timeLog = new TimeLog(
                user,
                lifeArea,
                request.logDate(),
                request.durationMinutes(),
                request.note(),
                request.source()
        );

        return toResponse(timeLogRepository.save(timeLog));
    }

    public TimeLogResponse update(Long id, UpdateTimeLogRequest request) {
        User user = currentUserService.getCurrentUser();

        TimeLog timeLog = timeLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Time log not found"));

        LifeArea lifeArea = getOwnedLifeArea(request.lifeAreaId(), user.getId());

        timeLog.update(
                lifeArea,
                request.logDate(),
                request.durationMinutes(),
                request.note(),
                request.source()
        );

        return toResponse(timeLogRepository.save(timeLog));
    }

    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();

        TimeLog timeLog = timeLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Time log not found"));

        timeLogRepository.delete(timeLog);
    }

    @Transactional(readOnly = true)
    public List<TimeLogResponse> findAll(
            LocalDate from,
            LocalDate to,
            Long lifeAreaId
    ) {
        User user = currentUserService.getCurrentUser();

        validateDateRange(from, to);

        List<TimeLog> timeLogs;

        if (lifeAreaId != null) {
            getOwnedLifeArea(lifeAreaId, user.getId());

            timeLogs = timeLogRepository
                    .findAllByUserIdAndLogDateBetweenAndLifeAreaIdOrderByLogDateDescIdDesc(
                            user.getId(),
                            from,
                            to,
                            lifeAreaId
                    );
        } else {
            timeLogs = timeLogRepository
                    .findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
                            user.getId(),
                            from,
                            to
                    );
        }

        return timeLogs.stream()
                .map(this::toResponse)
                .toList();
    }

    private LifeArea getOwnedLifeArea(Long lifeAreaId, Long userId) {
        return lifeAreaRepository.findByIdAndUserId(lifeAreaId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Life area not found"));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to dates are required");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }
    }

    private TimeLogResponse toResponse(TimeLog timeLog) {
        return new TimeLogResponse(
                timeLog.getId(),
                timeLog.getLifeArea().getId(),
                timeLog.getLifeArea().getName(),
                timeLog.getLogDate(),
                timeLog.getDurationMinutes(),
                timeLog.getNote(),
                timeLog.getSource(),
                timeLog.getCreatedAt()
        );
    }
}