package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Host;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HostRepository
        extends JpaRepository<Host, Long> {

    List<Host> findByFolderId(
            Long folderId
    );

    Optional<Host> findByHostnameAndFolderId(
            String hostname,
            Long folderId
    );

    boolean existsByHostnameAndFolderId(
            String hostname,
            Long folderId
    );
}