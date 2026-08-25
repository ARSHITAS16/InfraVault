package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.service.CredentialService;
import com.passwordmanager.backend.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    private final DeviceService deviceService;
    private final CredentialService credentialService;

    public DeviceController(DeviceService deviceService, CredentialService credentialService) {
        this.deviceService = deviceService;
        this.credentialService = credentialService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDevice(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(deviceService.getDevice(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id, Authentication authentication) {
        deviceService.deleteDevice(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/credentials")
    public ResponseEntity<List<Map<String, Object>>> getCredentials(@PathVariable Long id,
                                                                    @RequestParam Long datacenterId,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(credentialService.getMaskedCredentialsForDevice(id, datacenterId, authentication.getName()));
    }
}