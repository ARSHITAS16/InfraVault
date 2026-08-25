package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.InfrastructureFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InfrastructureFolderRepository
        extends JpaRepository<InfrastructureFolder, Long> {

    List<InfrastructureFolder> findByLocationId(
            Long locationId
    );

    Optional<InfrastructureFolder> findByNameAndLocationId(
            String name,
            Long locationId
    );

    boolean existsByNameAndLocationId(
            String name,
            Long locationId
    );
}