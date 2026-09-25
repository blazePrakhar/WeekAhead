package com.weekahead.audit.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.weekahead.audit.entity.AuditLog;
import com.weekahead.audit.repository.AuditLogRepository;
import com.weekahead.auth.entity.User;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(
            User user,
            String action,
            String entityType,
            Long entityId,
            String details
    ) {
        AuditLog auditLog = new AuditLog(
                user,
                action,
                entityType,
                entityId,
                details
        );

        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> findAllByUserId(Long userId) {
        return auditLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }
}