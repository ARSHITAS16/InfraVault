package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository
        extends JpaRepository<Folder, Long> {

    List<Folder> findByDatacenterId(Long datacenterId);

    Optional<Folder> findByNameAndDatacenterId(
            String name,
            Long datacenterId
    );

    boolean existsByNameAndDatacenterId(
            String name,
            Long datacenterId
    );
}