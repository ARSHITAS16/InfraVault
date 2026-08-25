package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
    List<AuditLog> findByDatacenterIdOrderByTimestampDesc(Long datacenterId);
    List<AuditLog> findTop50ByOrderByTimestampDesc();
}
