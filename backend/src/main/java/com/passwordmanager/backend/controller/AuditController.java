package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.AuditLog;
import com.passwordmanager.backend.entity.Datacenter;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import com.passwordmanager.backend.service.AuditService;
import com.passwordmanager.backend.service.DatacenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;
    private final DatacenterService datacenterService;

    public AuditController(AuditService auditService,
                           UserRepository userRepository,
                           DatacenterService datacenterService) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.datacenterService = datacenterService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Long datacenterId,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        String role = user.getRole() != null ? user.getRole().toUpperCase() : "VIEWER";
        if ("SUPER_ADMIN".equals(role)) {
            if (datacenterId != null) {
                return ResponseEntity.ok(auditService.getLogsForDatacenter(datacenterId));
            }
            return ResponseEntity.ok(auditService.getRecentLogs());
        }

        if ("DC_ADMIN".equals(role)) {
            if (datacenterId != null) {
                if (!datacenterService.hasAdminAccess(datacenterId, user.getId())) {
                    throw new SecurityException("Permission denied: Admin access required for this datacenter's audit logs");
                }
                return ResponseEntity.ok(auditService.getLogsForDatacenter(datacenterId));
            }
            List<Long> accessibleDcIds = datacenterService.getDatacentersForUser(user.getUsername())
                    .stream()
                    .map(Datacenter::getId)
                    .filter(dcId -> datacenterService.hasAdminAccess(dcId, user.getId()))
                    .toList();
            return ResponseEntity.ok(auditService.getLogsForDatacenters(accessibleDcIds));
        }

        throw new SecurityException("Permission denied: Audit logs require Administrator privileges");
    }
}
