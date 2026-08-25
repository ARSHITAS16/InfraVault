package com.passwordmanager.backend.dto;

public record PasswordEntryResponse(
        Long id,
        String websiteName,
        String websiteUrl,
        String username,
        String password,
        String notes
) {
}

