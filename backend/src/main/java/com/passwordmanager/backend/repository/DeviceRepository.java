package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository
        extends JpaRepository<Device, Long> {

    List<Device> findByFolderId(Long folderId);

    long countByFolderId(Long folderId);

    boolean existsByFolderId(Long folderId);

    java.util.Optional<Device> findByHostnameAndFolderId(String hostname, Long folderId);

    boolean existsByHostnameAndFolderId(String hostname, Long folderId);
}