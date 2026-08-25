package com.passwordmanager.backend.controller;

import com.passwordmanager.backend.service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/credentials")
@CrossOrigin(origins = "*")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping("/{id}/reveal")
    public ResponseEntity<Map<String, String>> revealCredential(@PathVariable Long id,
                                                                @RequestParam Long datacenterId,
                                                                Authentication authentication) {
        String decryptedSecret = credentialService.revealSecret(id, datacenterId, authentication.getName());
        return ResponseEntity.ok(Map.of("secret", decryptedSecret));
    }
}
