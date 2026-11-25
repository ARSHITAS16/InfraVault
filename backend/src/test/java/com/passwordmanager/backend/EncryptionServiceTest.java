package com.passwordmanager.backend;

import com.passwordmanager.backend.service.EncryptionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    @Test
    void encryptsWithUniqueIvAndDecryptsCorrectly() {
        EncryptionService svc = new EncryptionService("12345678901234567890123456789012");
        String plaintext = "TestInfraSecret!2026";

        String cipher1 = svc.encrypt(plaintext);
        String cipher2 = svc.encrypt(plaintext);

        assertNotEquals(plaintext, cipher1);
        assertNotEquals(cipher1, cipher2);
        assertEquals(plaintext, svc.decrypt(cipher1));
        assertEquals(plaintext, svc.decrypt(cipher2));
    }
}
