package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository
        extends JpaRepository<Location, Long> {

    List<Location> findByDatacenterId(
            Long datacenterId
    );

    Optional<Location> findByNameAndDatacenterId(
            String name,
            Long datacenterId
    );

    boolean existsByNameAndDatacenterId(
            String name,
            Long datacenterId
    );
}