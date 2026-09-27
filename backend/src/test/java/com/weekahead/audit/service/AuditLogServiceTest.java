package com.weekahead.audit.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weekahead.audit.entity.AuditLog;
import com.weekahead.audit.repository.AuditLogRepository;
import com.weekahead.auth.entity.User;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;

    private User user;

    @BeforeEach
    void setUp() {
        auditLogRepository = org.mockito.Mockito.mock(
                AuditLogRepository.class
        );

        auditLogService = new AuditLogService(
                auditLogRepository
        );

        user = org.mockito.Mockito.mock(User.class);

        when(user.getId()).thenReturn(1L);
    }

    @Test
    void shouldCreateAuditLog() {
        auditLogService.log(
                user,
                "LIFE_AREA_CREATED",
                "LIFE_AREA",
                10L,
                "Life area created"
        );

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository, times(1))
                .save(captor.capture());

        AuditLog savedAuditLog = captor.getValue();

        assertNotNull(savedAuditLog);
        assertEquals(user, savedAuditLog.getUser());
        assertEquals(
                "LIFE_AREA_CREATED",
                savedAuditLog.getAction()
        );
        assertEquals(
                "LIFE_AREA",
                savedAuditLog.getEntityType()
        );
        assertEquals(
                10L,
                savedAuditLog.getEntityId()
        );
        assertEquals(
                "Life area created",
                savedAuditLog.getDetails()
        );
    }

    @Test
    void shouldFindAuditLogsForUser() {
        AuditLog auditLog = new AuditLog(
                user,
                "TIME_LOG_CREATED",
                "TIME_LOG",
                20L,
                "Time log created"
        );

        when(auditLogRepository
                .findAllByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(auditLog));

        List<AuditLog> result =
                auditLogService.findAllByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(auditLog, result.get(0));

        verify(auditLogRepository, times(1))
                .findAllByUserIdOrderByCreatedAtDesc(1L);
    }
}