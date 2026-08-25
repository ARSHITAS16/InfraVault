package com.passwordmanager.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(
            @Value("${app.encryption.key}") String encryptionKey
    ) {

        if (encryptionKey.length() != 32) {
            throw new IllegalArgumentException(
                    "Encryption key must be exactly 32 characters long"
            );
        }

        this.secretKey = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8),
                "AES"
        );
    }

    public String encrypt(String plainText) {

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(TAG_LENGTH, iv);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] encryptedBytes =
                    cipher.doFinal(
                            plainText.getBytes(StandardCharsets.UTF_8)
                    );

            byte[] combined =
                    new byte[iv.length + encryptedBytes.length];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encryptedBytes,
                    0,
                    combined,
                    iv.length,
                    encryptedBytes.length
            );

            return Base64.getEncoder()
                    .encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to encrypt password",
                    e
            );
        }
    }

    public String decrypt(String encryptedText) {

        try {
            byte[] combined =
                    Base64.getDecoder()
                            .decode(encryptedText);

            byte[] iv =
                    new byte[IV_LENGTH];

            byte[] encryptedBytes =
                    new byte[combined.length - IV_LENGTH];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    encryptedBytes,
                    0,
                    encryptedBytes.length
            );

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(TAG_LENGTH, iv);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] decryptedBytes =
                    cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to decrypt password",
                    e
            );
        }
    }
}

