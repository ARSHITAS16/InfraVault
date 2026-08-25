package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByDeviceId(Long deviceId);
    Optional<Credential> findByDeviceIdAndType(Long deviceId, String type);
}
