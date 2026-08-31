package com.tutormgmt.authentication;

import com.tutormgmt.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-GCM encryption for OAuth tokens at rest (CLAUDE.md section 10.2).
 * Output format: base64( 12-byte IV || ciphertext || 16-byte GCM tag ).
 */
@Component
public class TokenCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(AppProperties props) {
        byte[] key = Base64.getDecoder().decode(props.crypto().tokenEncryptionKey());
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must decode to 16, 24 or 32 bytes");
        }
        this.keySpec = new SecretKeySpec(key, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt token", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored);
            byte[] iv = java.util.Arrays.copyOfRange(in, 0, IV_LENGTH);
            byte[] ct = java.util.Arrays.copyOfRange(in, IV_LENGTH, in.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt token", e);
        }
    }
}
