package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Credential;
import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.CredentialRepository;
import com.passwordmanager.backend.repository.DeviceRepository;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionService encryptionService;
    private final DatacenterService datacenterService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CredentialService(CredentialRepository credentialRepository,
                             DeviceRepository deviceRepository,
                             EncryptionService encryptionService,
                             DatacenterService datacenterService,
                             UserRepository userRepository,
                             AuditService auditService) {
        this.credentialRepository = credentialRepository;
        this.deviceRepository = deviceRepository;
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
        credential.setUpdatedAt(LocalDateTime.now());

        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential createCredentialAuthorized(Long deviceId, String type, String credUsername, String plainSecret, String actorUsername) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Credential type is required");
        }
        if (plainSecret == null || plainSecret.isBlank()) {
            throw new IllegalArgumentException("Credential secret is required");
        }

        User actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        Long actualDcId = device.getFolder().getDatacenter().getId();
        if (!datacenterService.hasWriteAccess(actualDcId, actor.getId())) {
            throw new SecurityException("Permission denied to create credentials in this datacenter");
        }

        String normalizedType = type.trim().toUpperCase();
        String normalizedUser = (credUsername != null && !credUsername.isBlank()) ? credUsername.trim() : "admin";
        Credential saved = saveCredential(device, normalizedType, normalizedUser, plainSecret, actor.getUsername());

        auditService.log(
                actor.getId(),
                actor.getUsername(),
                "CREATE_CREDENTIAL",
                "CREDENTIAL",
                saved.getId(),
                actualDcId,
                "Added encrypted credential type: " + normalizedType + " on device: " + device.getHostname()
        );

        return saved;
    }

    public List<Map<String, Object>> getMaskedCredentialsForDevice(Long deviceId, Long requestedDatacenterId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        Long actualDcId = device.getFolder().getDatacenter().getId();
        if (requestedDatacenterId != null && !actualDcId.equals(requestedDatacenterId)) {
            throw new SecurityException("Device does not belong to the requested datacenter");
        }

        if (!datacenterService.hasAccess(actualDcId, user.getId())) {
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
    public String revealSecret(Long credentialId, Long requestedDatacenterId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        Long actualDcId = credential.getDevice().getFolder().getDatacenter().getId();
        if (requestedDatacenterId != null && !actualDcId.equals(requestedDatacenterId)) {
            throw new SecurityException("Credential does not belong to the requested datacenter");
        }

        if (!datacenterService.hasAccess(actualDcId, user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        String decrypted = encryptionService.decrypt(credential.getEncryptedSecret());

        auditService.log(
                user.getId(),
                user.getUsername(),
                "REVEAL",
                "CREDENTIAL",
                credential.getId(),
                actualDcId,
                "Revealed credential type: " + credential.getType() + " for device: " + credential.getDevice().getHostname()
        );

        return decrypted;
    }

    @Transactional
    public void updateCredential(Long credentialId, Long requestedDatacenterId, String newPassword, String username) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        Long actualDcId = credential.getDevice().getFolder().getDatacenter().getId();
        if (requestedDatacenterId != null && !actualDcId.equals(requestedDatacenterId)) {
            throw new SecurityException("Credential does not belong to the requested datacenter");
        }

        if (!datacenterService.hasWriteAccess(actualDcId, user.getId())) {
            throw new SecurityException("Permission denied to update credentials in this datacenter");
        }

        String encrypted = encryptionService.encrypt(newPassword);
        credential.setEncryptedSecret(encrypted);
        credential.setUpdatedAt(LocalDateTime.now());
        credentialRepository.save(credential);

        auditService.log(
                user.getId(),
                user.getUsername(),
                "UPDATE_CREDENTIAL",
                "CREDENTIAL",
                credential.getId(),
                actualDcId,
                "Updated encrypted password for credential type: " + credential.getType() + " on device: " + credential.getDevice().getHostname()
        );
    }

    @Transactional
    public void deleteCredential(Long credentialId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        Long actualDcId = credential.getDevice().getFolder().getDatacenter().getId();
        if (!datacenterService.hasWriteAccess(actualDcId, user.getId())) {
            throw new SecurityException("Permission denied to delete credentials in this datacenter");
        }

        String type = credential.getType();
        String hostname = credential.getDevice().getHostname();
        credentialRepository.delete(credential);

        auditService.log(
                user.getId(),
                user.getUsername(),
                "DELETE_CREDENTIAL",
                "CREDENTIAL",
                credentialId,
                actualDcId,
                "Deleted credential type: " + type + " on device: " + hostname
        );
    }
}
