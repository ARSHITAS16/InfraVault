package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.AuditLog;
import com.passwordmanager.backend.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestParam(required = false) Long datacenterId) {
        if (datacenterId != null) {
            return ResponseEntity.ok(auditService.getLogsForDatacenter(datacenterId));
        }
        return ResponseEntity.ok(auditService.getRecentLogs());
    }
}
