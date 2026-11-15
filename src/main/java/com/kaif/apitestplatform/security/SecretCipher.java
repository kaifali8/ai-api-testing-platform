package com.kaif.apitestplatform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCipher {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();


    public SecretCipher(@Value("${app.encryption-key}") String base64Key) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key);
        }catch (IllegalArgumentException e){
            throw new IllegalStateException("app.encryption-key must be valid Base64");
        }
        if (raw.length != 32){
            throw new IllegalStateException("app.encryption-key must decode to exactly 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain){
        try {
            byte[] iv =new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(TAG_BITS,iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + ct.length).put(ct).array());
        }catch (GeneralSecurityException e){
            throw new IllegalStateException("Encryption failed",e);
        }
    }

    public String decrypt(String encoded){
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            if (all.length < IV_BYTES + TAG_BITS/8){
                throw new IllegalStateException("Stored secret is corrupted");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(TAG_BITS,all,0,IV_BYTES));
            byte[] pt = cipher.doFinal(all,IV_BYTES,all.length - IV_BYTES);
            return new String(pt,StandardCharsets.UTF_8);
        }catch (GeneralSecurityException | IllegalArgumentException e){
            throw new IllegalStateException("Decryption failed",e);
        }
    }
}
