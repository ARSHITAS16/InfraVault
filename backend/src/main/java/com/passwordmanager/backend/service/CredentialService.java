package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Credential;
import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.CredentialRepository;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final EncryptionService encryptionService;
    private final DatacenterService datacenterService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CredentialService(CredentialRepository credentialRepository,
                             EncryptionService encryptionService,
                             DatacenterService datacenterService,
                             UserRepository userRepository,
                             AuditService auditService) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
        this.datacenterService = datacenterService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Credential saveCredential(Device device, String type, String username, String plainSecret, String createdBy) {
        if (plainSecret == null || plainSecret.isBlank()) {
            return null;
        }

        String encrypted = encryptionService.encrypt(plainSecret);

        Credential credential = credentialRepository.findByDeviceIdAndType(device.getId(), type)
                .orElse(new Credential(device, type, username, encrypted, createdBy));

        credential.setUsername(username);
        credential.setEncryptedSecret(encrypted);
        credential.setCreatedBy(createdBy);

        return credentialRepository.save(credential);
    }

    public List<Map<String, Object>> getMaskedCredentialsForDevice(Long deviceId, Long datacenterId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!datacenterService.hasAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        List<Credential> credentials = credentialRepository.findByDeviceId(deviceId);

        return credentials.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("type", c.getType());
            map.put("username", c.getUsername());
            map.put("updatedAt", c.getUpdatedAt());
            map.put("maskedSecret", "••••••••••••");
            return map;
        }).toList();
    }

    @Transactional
    public String revealSecret(Long credentialId, Long datacenterId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!datacenterService.hasAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        String decrypted = encryptionService.decrypt(credential.getEncryptedSecret());

        // Audit log secret reveal
        auditService.log(
                user.getId(),
                user.getUsername(),
                "REVEAL",
                "CREDENTIAL",
                credential.getId(),
                datacenterId,
                "Revealed credential type: " + credential.getType() + " for device ID: " + credential.getDevice().getId()
        );

        return decrypted;
    }
}
