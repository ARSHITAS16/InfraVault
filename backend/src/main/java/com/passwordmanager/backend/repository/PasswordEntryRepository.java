package com.passwordmanager.backend.repository;

import com.passwordmanager.backend.entity.PasswordEntry;
import com.passwordmanager.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordEntryRepository
        extends JpaRepository<PasswordEntry, Long> {

    List<PasswordEntry> findByUser(User user);

    Optional<PasswordEntry> findByIdAndUser(Long id, User user);
}