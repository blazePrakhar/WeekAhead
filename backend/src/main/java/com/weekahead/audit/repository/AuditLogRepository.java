package com.weekahead.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weekahead.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}