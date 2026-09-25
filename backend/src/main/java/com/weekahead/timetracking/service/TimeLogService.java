package com.weekahead.timetracking.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weekahead.audit.service.AuditLogService;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.config.AnalyticsCacheInvalidationService;
import com.weekahead.config.DashboardCacheInvalidationService;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;
import com.weekahead.timetracking.dto.CreateTimeLogRequest;
import com.weekahead.timetracking.dto.TimeLogResponse;
import com.weekahead.timetracking.dto.UpdateTimeLogRequest;
import com.weekahead.timetracking.entity.TimeLog;
import com.weekahead.timetracking.repository.TimeLogRepository;

@Service
@Transactional
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final LifeAreaRepository lifeAreaRepository;
    private final CurrentUserService currentUserService;
    private final DashboardCacheInvalidationService dashboardCacheInvalidationService;
    private final AnalyticsCacheInvalidationService analyticsCacheInvalidationService;
    private final AuditLogService auditLogService;

    public TimeLogService(
            TimeLogRepository timeLogRepository,
            LifeAreaRepository lifeAreaRepository,
            CurrentUserService currentUserService,
            DashboardCacheInvalidationService dashboardCacheInvalidationService,
            AnalyticsCacheInvalidationService analyticsCacheInvalidationService,
            AuditLogService auditLogService
    ) {
        this.timeLogRepository = timeLogRepository;
        this.lifeAreaRepository = lifeAreaRepository;
        this.currentUserService = currentUserService;
        this.dashboardCacheInvalidationService = dashboardCacheInvalidationService;
        this.analyticsCacheInvalidationService = analyticsCacheInvalidationService;
        this.auditLogService = auditLogService;
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

        TimeLog savedTimeLog = timeLogRepository.save(timeLog);

        dashboardCacheInvalidationService.invalidate(user.getId());
        analyticsCacheInvalidationService.invalidate();

        auditLogService.log(
                user,
                "TIME_LOG_CREATED",
                "TIME_LOG",
                savedTimeLog.getId(),
                "Time log created"
        );

        return toResponse(savedTimeLog);
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

        TimeLog updatedTimeLog = timeLogRepository.save(timeLog);

        dashboardCacheInvalidationService.invalidate(user.getId());
        analyticsCacheInvalidationService.invalidate();

        auditLogService.log(
                user,
                "TIME_LOG_UPDATED",
                "TIME_LOG",
                updatedTimeLog.getId(),
                "Time log updated"
        );

        return toResponse(updatedTimeLog);
    }

    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();

        TimeLog timeLog = timeLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Time log not found"));

        timeLogRepository.delete(timeLog);

        dashboardCacheInvalidationService.invalidate(user.getId());
        analyticsCacheInvalidationService.invalidate();

        auditLogService.log(
                user,
                "TIME_LOG_DELETED",
                "TIME_LOG",
                timeLog.getId(),
                "Time log deleted"
        );
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