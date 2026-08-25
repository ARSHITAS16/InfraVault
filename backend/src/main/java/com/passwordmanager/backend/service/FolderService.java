package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Datacenter;
import com.passwordmanager.backend.entity.Folder;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.DatacenterRepository;
import com.passwordmanager.backend.repository.DeviceRepository;
import com.passwordmanager.backend.repository.FolderRepository;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final DatacenterRepository datacenterRepository;
    private final DeviceRepository deviceRepository;
    private final DatacenterService datacenterService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public FolderService(FolderRepository folderRepository,
                         DatacenterRepository datacenterRepository,
                         DeviceRepository deviceRepository,
                         DatacenterService datacenterService,
                         UserRepository userRepository,
                         AuditService auditService) {
        this.folderRepository = folderRepository;
        this.datacenterRepository = datacenterRepository;
        this.deviceRepository = deviceRepository;
        this.datacenterService = datacenterService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<Folder> getFoldersForDatacenter(Long datacenterId, String username) {
        User user = getUser(username);

        if (!datacenterService.hasAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied for this datacenter");
        }

        return folderRepository.findByDatacenterId(datacenterId);
    }

    @Transactional
    public Folder createFolder(Long datacenterId, String name, String username) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Folder name is required");
        }

        User user = getUser(username);

        if (!datacenterService.hasWriteAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied to create folder in this datacenter");
        }

        Datacenter datacenter = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new IllegalArgumentException("Datacenter not found"));

        String trimmedName = name.trim();
        if (folderRepository.existsByNameAndDatacenterId(trimmedName, datacenterId)) {
            throw new IllegalArgumentException("Folder with name '" + trimmedName + "' already exists in this datacenter");
        }

        Folder folder = new Folder(trimmedName, datacenter);
        Folder saved = folderRepository.save(folder);

        auditService.log(user.getId(), user.getUsername(), "CREATE_FOLDER", "FOLDER", saved.getId(), datacenterId, "Created folder: " + saved.getName());

        return saved;
    }

    @Transactional
    public void deleteFolder(Long folderId, String username, boolean force) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        User user = getUser(username);
        Long datacenterId = folder.getDatacenter().getId();

        if (!datacenterService.hasWriteAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied to delete folder in this datacenter");
        }

        long deviceCount = deviceRepository.countByFolderId(folderId);
        if (deviceCount > 0 && !force) {
            throw new IllegalStateException("Folder contains " + deviceCount + " devices. Delete or move all devices before deleting the folder.");
        }

        folderRepository.delete(folder);

        auditService.log(user.getId(), user.getUsername(), "DELETE_FOLDER", "FOLDER", folderId, datacenterId, "Deleted folder: " + folder.getName());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}