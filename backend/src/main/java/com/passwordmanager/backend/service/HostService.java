package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Host;
import com.passwordmanager.backend.entity.InfrastructureFolder;
import com.passwordmanager.backend.repository.HostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HostService {

    private final HostRepository hostRepository;
    private final InfrastructureFolderService folderService;

    public HostService(
            HostRepository hostRepository,
            InfrastructureFolderService folderService
    ) {
        this.hostRepository = hostRepository;
        this.folderService = folderService;
    }

    // =========================================================
    // GET ALL HOSTS FOR A FOLDER
    // =========================================================

    public List<Host> getHosts(
            Long folderId,
            Long userId
    ) {

        InfrastructureFolder folder =
                folderService.getFolder(
                        folderId,
                        userId
                );

        return hostRepository.findByFolderId(
                folder.getId()
        );
    }

    // =========================================================
    // GET SINGLE HOST
    // =========================================================

    public Host getHost(
            Long hostId,
            Long userId
    ) {

        Host host =
                hostRepository.findById(hostId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Host not found"
                                )
                        );

        folderService.getFolder(
                host.getFolder().getId(),
                userId
        );

        return host;
    }

    // =========================================================
    // CREATE HOST
    // =========================================================

    @Transactional
    public Host createHost(
            Long folderId,
            Long userId,
            String hostname
    ) {

        InfrastructureFolder folder =
                folderService.getFolder(
                        folderId,
                        userId
                );

        Long datacenterId =
                folder.getLocation()
                        .getDatacenter()
                        .getId();

        if (!folderService
                .getDatacenterService()
                .hasWriteAccess(
                        datacenterId,
                        userId
                )) {

            throw new SecurityException(
                    "Write permission required"
            );
        }

        if (hostname == null || hostname.isBlank()) {

            throw new IllegalArgumentException(
                    "Hostname is required"
            );
        }

        String trimmedHostname =
                hostname.trim();

        if (hostRepository
                .existsByHostnameAndFolderId(
                        trimmedHostname,
                        folderId
                )) {

            throw new IllegalArgumentException(
                    "Host already exists in this folder"
            );
        }

        Host host =
                new Host(
                        trimmedHostname,
                        folder
                );

        return hostRepository.save(
                host
        );
    }

    // =========================================================
    // DELETE HOST
    // =========================================================

    @Transactional
    public void deleteHost(
            Long hostId,
            Long userId
    ) {

        Host host =
                getHost(
                        hostId,
                        userId
                );

        Long datacenterId =
                host.getFolder()
                        .getLocation()
                        .getDatacenter()
                        .getId();

        if (!folderService
                .getDatacenterService()
                .hasWriteAccess(
                        datacenterId,
                        userId
                )) {

            throw new SecurityException(
                    "Write permission required"
            );
        }

        hostRepository.delete(
                host
        );
    }
}