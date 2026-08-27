package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByDeviceId(Long deviceId);
    Optional<Credential> findByDeviceIdAndType(Long deviceId, String type);

    @Modifying
    @Query("DELETE FROM Credential c WHERE c.device.id = :deviceId")
    void deleteByDeviceId(@Param("deviceId") Long deviceId);
}
