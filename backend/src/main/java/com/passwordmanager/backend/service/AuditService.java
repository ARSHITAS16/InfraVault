package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.AuditLog;
import com.passwordmanager.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog log(Long userId, String username, String action, String entityType, Long entityId, Long datacenterId, String metadata) {
        AuditLog log = new AuditLog(userId, username, action, entityType, entityId, datacenterId, metadata);
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsForDatacenter(Long datacenterId) {
        return auditLogRepository.findByDatacenterIdOrderByTimestampDesc(datacenterId);
    }
}
