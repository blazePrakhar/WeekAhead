package com.weekahead.week.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.config.DashboardCacheInvalidationService;
import com.weekahead.week.dto.WeekRequest;
import com.weekahead.week.dto.WeekResponse;
import com.weekahead.week.entity.Week;
import com.weekahead.week.repository.WeekRepository;

@Service
public class WeekService {

    private final WeekRepository weekRepository;
    private final CurrentUserService currentUserService;
    private final DashboardCacheInvalidationService dashboardCacheInvalidationService;

    public WeekService(
            WeekRepository weekRepository,
            CurrentUserService currentUserService,
            DashboardCacheInvalidationService dashboardCacheInvalidationService
    ) {
        this.weekRepository = weekRepository;
        this.currentUserService = currentUserService;
        this.dashboardCacheInvalidationService = dashboardCacheInvalidationService;
    }

    public WeekResponse create(WeekRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request.fixedCommitmentMinutes() > request.availableMinutes()) {
            throw new IllegalArgumentException(
                    "Fixed commitment minutes must be less than or equal to available minutes"
            );
        }

        LocalDate weekStartDate = request.weekStartDate();
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        if (weekRepository
                .findByUserIdAndWeekStartDate(
                        currentUser.getId(),
                        weekStartDate
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "A week already exists for the given start date"
            );
        }

        Week week = new Week(
                currentUser,
                weekStartDate,
                weekEndDate,
                request.availableMinutes(),
                request.fixedCommitmentMinutes(),
                null
        );

        Week savedWeek = weekRepository.save(week);

        dashboardCacheInvalidationService.invalidate(currentUser.getId());

        return toResponse(savedWeek);
    }

    public WeekResponse getCurrent() {
        User currentUser = currentUserService.getCurrentUser();

        LocalDate today = LocalDate.now();

        Week week = weekRepository
                .findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
                        currentUser.getId(),
                        today,
                        today
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Current week not found"
                ));

        return toResponse(week);
    }

    public WeekResponse getById(Long id) {
        User currentUser = currentUserService.getCurrentUser();

        Week week = weekRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Week not found"
                ));

        return toResponse(week);
    }

    public List<Week> getLatestCompletedWeeks() {
        User currentUser = currentUserService.getCurrentUser();

        return weekRepository
                .findByUserIdAndWeekEndDateBeforeOrderByWeekEndDateDesc(
                        currentUser.getId(),
                        LocalDate.now(),
                        PageRequest.of(0, 4)
                );
    }

    private WeekResponse toResponse(Week week) {
        return new WeekResponse(
                week.getId(),
                week.getWeekStartDate(),
                week.getWeekEndDate(),
                week.getAvailableMinutes(),
                week.getFixedCommitmentMinutes()
        );
    }
}