package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.entity.DeviceField;
import com.passwordmanager.backend.entity.Folder;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.CredentialRepository;
import com.passwordmanager.backend.repository.DeviceFieldRepository;
import com.passwordmanager.backend.repository.DeviceRepository;
import com.passwordmanager.backend.repository.FolderRepository;
import com.passwordmanager.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FolderRepository folderRepository;
    private final DatacenterService datacenterService;
    private final CredentialService credentialService;
    private final CredentialRepository credentialRepository;
    private final DeviceFieldRepository deviceFieldRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    public DeviceService(DeviceRepository deviceRepository,
                         FolderRepository folderRepository,
                         DatacenterService datacenterService,
                         CredentialService credentialService,
                         CredentialRepository credentialRepository,
                         DeviceFieldRepository deviceFieldRepository,
                         UserRepository userRepository,
                         AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.folderRepository = folderRepository;
        this.datacenterService = datacenterService;
        this.credentialService = credentialService;
        this.credentialRepository = credentialRepository;
        this.deviceFieldRepository = deviceFieldRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<Device> getDevicesForFolder(Long folderId, String username) {
        Folder folder = getFolder(folderId);
        User user = getUser(username);

        if (!datacenterService.hasAccess(folder.getDatacenter().getId(), user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        return deviceRepository.findByFolderId(folderId);
    }

    public Device getDevice(Long deviceId, String username) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        User user = getUser(username);
        if (!datacenterService.hasAccess(device.getFolder().getDatacenter().getId(), user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        return device;
    }

    public List<Map<String, Object>> getDeviceFields(Long deviceId, String username) {
        Device device = getDevice(deviceId, username);
        return deviceFieldRepository.findByDeviceId(device.getId()).stream().map(df -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", df.getId());
            m.put("fieldName", df.getFieldName());
            m.put("fieldValue", df.getFieldValue());
            return m;
        }).toList();
    }

    @Transactional
    public Device createDevice(Long folderId,
                               String hostname,
                               String model,
                               String serialNumber,
                               String capacity,
                               String serviceTag,
                               LocalDate supportEndDate,
                               String consolePort,
                               Boolean idracConfigured,
                               String idracPassword,
                               String sysadminPassword,
                               String secoffPassword,
                               String passphrase,
                               Map<String, String> customFields,
                               String username) {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Hostname is required");
        }

        Folder folder = getFolder(folderId);
        User user = getUser(username);
        Long datacenterId = folder.getDatacenter().getId();

        if (!datacenterService.hasWriteAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied to create device in this datacenter");
        }

        String trimmedHostname = hostname.trim();
        if (deviceRepository.existsByHostnameAndFolderId(trimmedHostname, folderId)) {
            throw new IllegalArgumentException("Device with hostname '" + trimmedHostname + "' already exists in this folder");
        }

        Device device = new Device(trimmedHostname, model, serialNumber, capacity, serviceTag, supportEndDate, folder);
        device.setConsolePort(consolePort);
        device.setIdracConfigured(idracConfigured != null ? idracConfigured : false);

        Device savedDevice = deviceRepository.save(device);

        if (customFields != null) {
            for (Map.Entry<String, String> entry : customFields.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()) {
                    deviceFieldRepository.save(new DeviceField(entry.getKey().trim(), entry.getValue(), savedDevice));
                }
            }
        }

        credentialService.saveCredential(savedDevice, "IDRAC", "root", idracPassword, username);
        credentialService.saveCredential(savedDevice, "SYSADMIN", "sysadmin", sysadminPassword, username);
        credentialService.saveCredential(savedDevice, "SECOFF", "secoff", secoffPassword, username);
        credentialService.saveCredential(savedDevice, "PASSPHRASE", "admin", passphrase, username);

        auditService.log(user.getId(), user.getUsername(), "CREATE_DEVICE", "DEVICE", savedDevice.getId(), datacenterId,
                "Created device: " + savedDevice.getHostname());

        return savedDevice;
    }

    @Transactional
    public void deleteDevice(Long deviceId, String username) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        User user = getUser(username);
        Long datacenterId = device.getFolder().getDatacenter().getId();

        if (!datacenterService.hasWriteAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied to delete device in this datacenter");
        }

        entityManager.createNativeQuery("DELETE FROM credentials WHERE device_id = :deviceId")
                .setParameter("deviceId", deviceId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM device_fields WHERE device_id = :deviceId")
                .setParameter("deviceId", deviceId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM devices WHERE id = :deviceId")
                .setParameter("deviceId", deviceId)
                .executeUpdate();

        auditService.log(user.getId(), user.getUsername(), "DELETE_DEVICE", "DEVICE", deviceId, datacenterId,
                "Deleted device: " + device.getHostname());
    }

    private Folder getFolder(Long folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
