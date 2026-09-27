package com.weekahead.lifearea.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weekahead.lifearea.entity.LifeArea;

public interface LifeAreaRepository extends JpaRepository<LifeArea, Long> {

    List<LifeArea> findAllByUserId(Long userId);

    Optional<LifeArea> findByIdAndUserId(Long id, Long userId);
}
