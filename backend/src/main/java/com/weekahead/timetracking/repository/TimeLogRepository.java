package com.weekahead.timetracking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weekahead.timetracking.entity.TimeLog;

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

    @Query("""
            SELECT tl.lifeArea.id, SUM(tl.durationMinutes)
            FROM TimeLog tl
            WHERE tl.user.id = :userId
              AND tl.logDate BETWEEN :from AND :to
            GROUP BY tl.lifeArea.id
            """)
    List<Object[]> sumDurationByLifeArea(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
