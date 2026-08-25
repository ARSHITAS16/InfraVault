package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.DatacenterUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatacenterUserRepository
        extends JpaRepository<DatacenterUser, Long> {

    Optional<DatacenterUser> findByDatacenterIdAndUserId(
            Long datacenterId,
            Long userId
    );

    List<DatacenterUser> findByDatacenterId(
            Long datacenterId
    );

    List<DatacenterUser> findByUserId(
            Long userId
    );

    boolean existsByDatacenterIdAndUserId(
            Long datacenterId,
            Long userId
    );
}