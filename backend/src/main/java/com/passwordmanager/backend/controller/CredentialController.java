package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.repository.DeviceRepository;
import com.passwordmanager.backend.service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/credentials")
@CrossOrigin(origins = "*")
public class CredentialController {

    private final CredentialService credentialService;
    private final DeviceRepository deviceRepository;

    public CredentialController(CredentialService credentialService, DeviceRepository deviceRepository) {
        this.credentialService = credentialService;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping("/{id}/reveal")
    public ResponseEntity<Map<String, String>> revealCredential(@PathVariable Long id,
                                                                @RequestParam Long datacenterId,
                                                                Authentication authentication) {
        String decryptedSecret = credentialService.revealSecret(id, datacenterId, authentication.getName());
        return ResponseEntity.ok(Map.of("secret", decryptedSecret));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCredential(@RequestBody Map<String, Object> payload,
                                                                Authentication authentication) {
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        String type = payload.get("type").toString();
        String username = payload.get("username") != null ? payload.get("username").toString() : "admin";
        String password = payload.get("password") != null ? payload.get("password").toString() : "";

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        credentialService.saveCredential(device, type.toUpperCase(), username, password, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Credential created successfully"));
    }
}
