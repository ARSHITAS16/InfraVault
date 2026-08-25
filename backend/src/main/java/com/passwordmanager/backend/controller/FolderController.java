package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.service.DeviceService;
import com.passwordmanager.backend.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
@CrossOrigin(origins = "*")
public class FolderController {

    private final FolderService folderService;
    private final DeviceService deviceService;

    public FolderController(FolderService folderService, DeviceService deviceService) {
        this.folderService = folderService;
        this.deviceService = deviceService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id,
                                             @RequestParam(defaultValue = "false") boolean force,
                                             Authentication authentication) {
        folderService.deleteFolder(id, authentication.getName(), force);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/devices")
    public ResponseEntity<List<Device>> getDevices(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(deviceService.getDevicesForFolder(id, authentication.getName()));
    }

    @PostMapping("/{id}/devices")
    public ResponseEntity<Device> createDevice(@PathVariable Long id,
                                               Authentication authentication,
                                               @RequestBody CreateDeviceRequest request) {
        LocalDate supportDate = null;
        if (request.getSupportEndDate() != null && !request.getSupportEndDate().isBlank()) {
            supportDate = LocalDate.parse(request.getSupportEndDate());
        }

        Device device = deviceService.createDevice(
                id,
                request.getHostname(),
                request.getModel(),
                request.getSerialNumber(),
                request.getCapacity(),
                request.getServiceTag(),
                supportDate,
                request.getConsolePort(),
                request.getIdracConfigured(),
                request.getIdracPassword(),
                request.getSysadminPassword(),
                request.getSecoffPassword(),
                request.getPassphrase(),
                authentication.getName()
        );

        return ResponseEntity.ok(device);
    }

    public static class CreateDeviceRequest {
        private String hostname;
        private String model;
        private String serialNumber;
        private String capacity;
        private String serviceTag;
        private String supportEndDate;
        private String consolePort;
        private Boolean idracConfigured;

        private String idracPassword;
        private String sysadminPassword;
        private String secoffPassword;
        private String passphrase;

        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public String getCapacity() { return capacity; }
        public void setCapacity(String capacity) { this.capacity = capacity; }
        public String getServiceTag() { return serviceTag; }
        public void setServiceTag(String serviceTag) { this.serviceTag = serviceTag; }
        public String getSupportEndDate() { return supportEndDate; }
        public void setSupportEndDate(String supportEndDate) { this.supportEndDate = supportEndDate; }
        public String getConsolePort() { return consolePort; }
        public void setConsolePort(String consolePort) { this.consolePort = consolePort; }
        public Boolean getIdracConfigured() { return idracConfigured; }
        public void setIdracConfigured(Boolean idracConfigured) { this.idracConfigured = idracConfigured; }
        public String getIdracPassword() { return idracPassword; }
        public void setIdracPassword(String idracPassword) { this.idracPassword = idracPassword; }
        public String getSysadminPassword() { return sysadminPassword; }
        public void setSysadminPassword(String sysadminPassword) { this.sysadminPassword = sysadminPassword; }
        public String getSecoffPassword() { return secoffPassword; }
        public void setSecoffPassword(String secoffPassword) { this.secoffPassword = secoffPassword; }
        public String getPassphrase() { return passphrase; }
        public void setPassphrase(String passphrase) { this.passphrase = passphrase; }
    }
}
