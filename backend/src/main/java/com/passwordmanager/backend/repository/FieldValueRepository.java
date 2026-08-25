package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.FieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldValueRepository
        extends JpaRepository<FieldValue, Long> {

    List<FieldValue> findByHostId(
            Long hostId
    );

    void deleteByHostId(
            Long hostId
    );
}