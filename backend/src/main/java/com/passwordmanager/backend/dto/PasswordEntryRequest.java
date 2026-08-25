package com.passwordmanager.backend.dto;

public record PasswordEntryRequest(
        String websiteName,
        String websiteUrl,
        String username,
        String password,
        String notes
) {
}

