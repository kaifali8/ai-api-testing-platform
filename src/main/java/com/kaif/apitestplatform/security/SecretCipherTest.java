package com.kaif.apitestplatform.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

    private static String keyOf(byte fill, int length) {
        byte[] k = new byte[length];
        java.util.Arrays.fill(k, fill);
        return Base64.getEncoder().encodeToString(k);
    }

    private final SecretCipher cipher = new SecretCipher(keyOf((byte) 1, 32));

    @Test
    void roundTripReturnsOriginal() {
        assertThat(cipher.decrypt(cipher.encrypt("my-token-123"))).isEqualTo("my-token-123");
    }

    @Test
    void sameInputGivesDifferentCiphertext() {
        assertThat(cipher.encrypt("x")).isNotEqualTo(cipher.encrypt("x"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt("secret"));
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void differentKeyCannotDecrypt() {
        String encrypted = cipher.encrypt("secret");
        SecretCipher other = new SecretCipher(keyOf((byte) 2, 32));
        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyLengthFailsFast() {
        assertThatThrownBy(() -> new SecretCipher(keyOf((byte) 1, 16)))
                .isInstanceOf(IllegalStateException.class);
    }
}