package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.dto.PasswordEntryRequest;
import com.passwordmanager.backend.dto.PasswordEntryResponse;
import com.passwordmanager.backend.service.PasswordEntryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passwords")
public class PasswordEntryController {

    private final PasswordEntryService passwordEntryService;

    public PasswordEntryController(
            PasswordEntryService passwordEntryService
    ) {
        this.passwordEntryService = passwordEntryService;
    }

    @PostMapping
    public PasswordEntryResponse create(
            Authentication authentication,
            @RequestBody PasswordEntryRequest request
    ) {

        return passwordEntryService.create(
                authentication.getName(),
                request
        );
    }

    @GetMapping
    public List<PasswordEntryResponse> getAll(
            Authentication authentication
    ) {

        return passwordEntryService.getAll(
                authentication.getName()
        );
    }

    @GetMapping("/{id}")
    public PasswordEntryResponse getById(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return passwordEntryService.getById(
                authentication.getName(),
                id
        );
    }

    @PutMapping("/{id}")
    public PasswordEntryResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody PasswordEntryRequest request
    ) {

        return passwordEntryService.update(
                authentication.getName(),
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public void delete(
            Authentication authentication,
            @PathVariable Long id
    ) {

        passwordEntryService.delete(
                authentication.getName(),
                id
        );
    }
}

