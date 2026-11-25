package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping("/{id}/reveal")
    public ResponseEntity<Map<String, String>> revealCredential(@PathVariable Long id,
                                                                @RequestParam(required = false) Long datacenterId,
                                                                Authentication authentication) {
        String decryptedSecret = credentialService.revealSecret(id, datacenterId, authentication.getName());
        return ResponseEntity.ok(Map.of("secret", decryptedSecret));
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<Map<String, String>> updateCredentialPost(@PathVariable Long id,
                                                                    @RequestBody Map<String, Object> payload,
                                                                    Authentication authentication) {
        return handleUpdate(id, payload, authentication);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateCredentialPut(@PathVariable Long id,
                                                                   @RequestBody Map<String, Object> payload,
                                                                   Authentication authentication) {
        return handleUpdate(id, payload, authentication);
    }

    private ResponseEntity<Map<String, String>> handleUpdate(Long id, Map<String, Object> payload, Authentication authentication) {
        Long datacenterId = payload.get("datacenterId") != null ? Long.valueOf(payload.get("datacenterId").toString()) : null;
        String newPassword = payload.get("newPassword") != null
                ? payload.get("newPassword").toString()
                : (payload.get("password") != null ? payload.get("password").toString() : "");

        credentialService.updateCredential(id, datacenterId, newPassword, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCredential(@RequestBody Map<String, Object> payload,
                                                                Authentication authentication) {
        if (payload.get("deviceId") == null || payload.get("type") == null) {
            throw new IllegalArgumentException("deviceId and type are required");
        }
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        String type = payload.get("type").toString();
        String username = payload.get("username") != null ? payload.get("username").toString() : "admin";
        String password = payload.get("password") != null ? payload.get("password").toString() : "";

        credentialService.createCredentialAuthorized(deviceId, type, username, password, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Credential created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredential(@PathVariable Long id, Authentication authentication) {
        credentialService.deleteCredential(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
