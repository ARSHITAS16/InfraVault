package com.passwordmanager.backend.service;

import com.passwordmanager.backend.dto.PasswordEntryRequest;
import com.passwordmanager.backend.dto.PasswordEntryResponse;
import com.passwordmanager.backend.entity.PasswordEntry;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.PasswordEntryRepository;
import com.passwordmanager.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordEntryService {

    private final PasswordEntryRepository passwordEntryRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public PasswordEntryService(
            PasswordEntryRepository passwordEntryRepository,
            UserRepository userRepository,
            EncryptionService encryptionService
    ) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public PasswordEntryResponse create(
            String username,
            PasswordEntryRequest request
    ) {

        User user = getUser(username);

        String encryptedPassword =
                encryptionService.encrypt(request.password());

        PasswordEntry entry = new PasswordEntry(
                request.websiteName(),
                request.websiteUrl(),
                request.username(),
                encryptedPassword,
                request.notes(),
                user
        );

        PasswordEntry saved =
                passwordEntryRepository.save(entry);

        return toResponse(saved);
    }

    public List<PasswordEntryResponse> getAll(String username) {

        User user = getUser(username);

        return passwordEntryRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PasswordEntryResponse getById(
            String username,
            Long id
    ) {

        User user = getUser(username);

        PasswordEntry entry =
                passwordEntryRepository
                        .findByIdAndUser(id, user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Password entry not found"
                                )
                        );

        return toResponse(entry);
    }

    public PasswordEntryResponse update(
            String username,
            Long id,
            PasswordEntryRequest request
    ) {

        User user = getUser(username);

        PasswordEntry entry =
                passwordEntryRepository
                        .findByIdAndUser(id, user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Password entry not found"
                                )
                        );

        entry.setWebsiteName(request.websiteName());
        entry.setWebsiteUrl(request.websiteUrl());
        entry.setUsername(request.username());
        entry.setEncryptedPassword(
                encryptionService.encrypt(request.password())
        );
        entry.setNotes(request.notes());

        PasswordEntry updated =
                passwordEntryRepository.save(entry);

        return toResponse(updated);
    }

    public void delete(
            String username,
            Long id
    ) {

        User user = getUser(username);

        PasswordEntry entry =
                passwordEntryRepository
                        .findByIdAndUser(id, user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Password entry not found"
                                )
                        );

        passwordEntryRepository.delete(entry);
    }

    private User getUser(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }

    private PasswordEntryResponse toResponse(
            PasswordEntry entry
    ) {

        String decryptedPassword =
                encryptionService.decrypt(
                        entry.getEncryptedPassword()
                );

        return new PasswordEntryResponse(
                entry.getId(),
                entry.getWebsiteName(),
                entry.getWebsiteUrl(),
                entry.getUsername(),
                decryptedPassword,
                entry.getNotes()
        );
    }
}

