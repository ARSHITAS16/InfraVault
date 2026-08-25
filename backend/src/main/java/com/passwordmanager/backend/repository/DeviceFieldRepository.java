package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.DeviceField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceFieldRepository
        extends JpaRepository<DeviceField, Long> {

    List<DeviceField> findByDeviceId(Long deviceId);

    void deleteByDeviceId(Long deviceId);
}