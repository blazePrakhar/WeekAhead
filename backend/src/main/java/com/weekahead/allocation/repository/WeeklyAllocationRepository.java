package com.weekahead.allocation.repository;

import com.weekahead.allocation.entity.WeeklyAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyAllocationRepository
        extends JpaRepository<WeeklyAllocation, Long> {

    List<WeeklyAllocation> findAllByWeekIdOrderByLifeAreaIdAsc(Long weekId);

    Optional<WeeklyAllocation> findByWeekIdAndLifeAreaId(
            Long weekId,
            Long lifeAreaId
    );

    List<WeeklyAllocation> findAllByWeekIdInOrderByWeekIdAscLifeAreaIdAsc(
            List<Long> weekIds
    );

    long deleteByWeekIdAndLifeAreaIdNotIn(
            Long weekId,
            List<Long> lifeAreaIds
    );
}
