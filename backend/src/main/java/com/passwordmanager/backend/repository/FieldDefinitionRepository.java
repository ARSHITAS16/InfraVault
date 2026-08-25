package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldDefinitionRepository
        extends JpaRepository<FieldDefinition, Long> {

    List<FieldDefinition> findByFolderId(
            Long folderId
    );
}