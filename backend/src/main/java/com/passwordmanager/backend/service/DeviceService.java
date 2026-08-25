package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.entity.Folder;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.DeviceRepository;
import com.passwordmanager.backend.repository.FolderRepository;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FolderRepository folderRepository;
    private final DatacenterService datacenterService;
    private final CredentialService credentialService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DeviceService(DeviceRepository deviceRepository,
                         FolderRepository folderRepository,
                         DatacenterService datacenterService,
                         CredentialService credentialService,
                         UserRepository userRepository,
                         AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.folderRepository = folderRepository;
        this.datacenterService = datacenterService;
        this.credentialService = credentialService;
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

        // Save encrypted credentials
        credentialService.saveCredential(savedDevice, "IDRAC", "root", idracPassword, username);
        credentialService.saveCredential(savedDevice, "SYSADMIN", "sysadmin", sysadminPassword, username);
        credentialService.saveCredential(savedDevice, "SECOFF", "secoff", secoffPassword, username);
        credentialService.saveCredential(savedDevice, "PASSPHRASE", "admin", passphrase, username);

        auditService.log(user.getId(), user.getUsername(), "CREATE_DEVICE", "DEVICE", savedDevice.getId(), datacenterId, "Created device: " + savedDevice.getHostname());

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

        deviceRepository.delete(device);

        auditService.log(user.getId(), user.getUsername(), "DELETE_DEVICE", "DEVICE", deviceId, datacenterId, "Deleted device: " + device.getHostname());
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