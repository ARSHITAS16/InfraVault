package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Datacenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatacenterRepository
        extends JpaRepository<Datacenter, Long> {

    Optional<Datacenter> findByName(String name);

    boolean existsByName(String name);
}