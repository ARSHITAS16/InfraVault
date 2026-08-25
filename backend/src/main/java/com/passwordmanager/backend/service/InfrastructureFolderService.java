package com.passwordmanager.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passwordmanager.backend.entity.InfrastructureFolder;
import com.passwordmanager.backend.entity.Location;
import com.passwordmanager.backend.repository.InfrastructureFolderRepository;

@Service
public class InfrastructureFolderService {

    private final InfrastructureFolderRepository folderRepository;
    private final LocationService locationService;

    public InfrastructureFolderService(
            InfrastructureFolderRepository folderRepository,
            LocationService locationService
    ) {
        this.folderRepository = folderRepository;
        this.locationService = locationService;
    }

    // =========================================================
    // GET DATACENTER SERVICE
    // Used by HostService for permission checks
    // =========================================================

    public DatacenterService getDatacenterService() {
        return locationService.getDatacenterService();
    }

    // =========================================================
    // GET ALL FOLDERS FOR A LOCATION
    // =========================================================

    public List<InfrastructureFolder> getFolders(
            Long locationId,
            Long userId
    ) {

        Location location =
                locationService.getLocation(
                        locationId,
                        userId
                );

        return folderRepository.findByLocationId(
                location.getId()
        );
    }

    // =========================================================
    // GET SINGLE FOLDER
    // =========================================================

    public InfrastructureFolder getFolder(
            Long folderId,
            Long userId
    ) {

        InfrastructureFolder folder =
                folderRepository.findById(folderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Folder not found"
                                )
                        );

        locationService.getLocation(
                folder.getLocation().getId(),
                userId
        );

        return folder;
    }

    // =========================================================
    // CREATE FOLDER
    // =========================================================

    @Transactional
    public InfrastructureFolder createFolder(
            Long locationId,
            Long userId,
            String name
    ) {

        Location location =
                locationService.getLocation(
                        locationId,
                        userId
                );

        Long datacenterId =
                location.getDatacenter().getId();

        if (!locationService
                .getDatacenterService()
                .hasWriteAccess(
                        datacenterId,
                        userId
                )) {

            throw new SecurityException(
                    "Write permission required"
            );
        }

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Folder name is required"
            );
        }

        String trimmedName =
                name.trim();

        if (folderRepository
                .existsByNameAndLocationId(
                        trimmedName,
                        locationId
                )) {

            throw new IllegalArgumentException(
                    "Folder already exists in this location"
            );
        }

        InfrastructureFolder folder =
                new InfrastructureFolder(
                        trimmedName,
                        location
                );

        return folderRepository.save(
                folder
        );
    }

    // =========================================================
    // DELETE FOLDER
    // =========================================================

    @Transactional
    public void deleteFolder(
            Long folderId,
            Long userId
    ) {

        InfrastructureFolder folder =
                getFolder(
                        folderId,
                        userId
                );

        Long datacenterId =
                folder.getLocation()
                        .getDatacenter()
                        .getId();

        if (!locationService
                .getDatacenterService()
                .hasWriteAccess(
                        datacenterId,
                        userId
                )) {

            throw new SecurityException(
                    "Write permission required"
            );
        }

        folderRepository.delete(
                folder
        );
    }
}