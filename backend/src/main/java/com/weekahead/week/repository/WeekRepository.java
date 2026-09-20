package com.weekahead.week.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weekahead.week.entity.Week;

public interface WeekRepository extends JpaRepository<Week, Long> {

    List<Week> findAllByUserId(Long userId);

    Optional<Week> findByIdAndUserId(Long id, Long userId);

    Optional<Week> findByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    Optional<Week> findByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
            Long userId,
            LocalDate date,
            LocalDate sameDate
    );
}