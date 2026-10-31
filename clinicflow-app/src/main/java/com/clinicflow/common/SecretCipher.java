package com.clinicflow.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Small AES-GCM helper for at-rest secrets such as SMTP passwords. The key is
 * derived from the deployment secret; changing that secret makes old ciphertext
 * unreadable, so operators must re-enter SMTP credentials after rotation.
 */
@Component
public class SecretCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(@Value("${clinicflow.security.secret-key:}") String configured,
                        @Value("${clinicflow.security.remember-me-key:local-only-key}") String fallback) {
        String material = configured == null || configured.isBlank() ? fallback : configured;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not initialise the secret cipher", ex);
        }
    }

    public byte[] encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt the secret", ex);
        }
    }

    public String decrypt(byte[] value) {
        if (value == null || value.length <= IV_BYTES) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(value, 0, iv, 0, IV_BYTES);
            byte[] ciphertext = new byte[value.length - IV_BYTES];
            System.arraycopy(value, IV_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    public String fingerprint(byte[] value) {
        if (value == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value).substring(0, 6);
    }
}
