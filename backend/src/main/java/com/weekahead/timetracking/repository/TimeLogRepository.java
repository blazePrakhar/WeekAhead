package com.weekahead.timetracking.repository;

import com.weekahead.timetracking.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    Optional<TimeLog> findByIdAndUserId(Long id, Long userId);

    List<TimeLog> findAllByUserIdAndLogDateBetweenOrderByLogDateDescIdDesc(
            Long userId,
            LocalDate from,
            LocalDate to
    );

    List<TimeLog> findAllByUserIdAndLogDateBetweenAndLifeAreaIdOrderByLogDateDescIdDesc(
            Long userId,
            LocalDate from,
            LocalDate to,
            Long lifeAreaId
    );
}