package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.DatacenterUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatacenterUserRepository
        extends JpaRepository<DatacenterUser, Long> {

    Optional<DatacenterUser> findByDatacenterIdAndUserId(
            Long datacenterId,
            Long userId
    );

    @Query("SELECT du FROM DatacenterUser du JOIN FETCH du.user JOIN FETCH du.datacenter WHERE du.datacenter.id = :datacenterId")
    List<DatacenterUser> findByDatacenterId(
            @Param("datacenterId") Long datacenterId
    );

    List<DatacenterUser> findByUserId(
            Long userId
    );

    boolean existsByDatacenterIdAndUserId(
            Long datacenterId,
            Long userId
    );
}