package com.kaif.apitestplatform.security;

import com.kaif.apitestplatform.exception.UnsafeUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UrlGuardTest {

    private final UrlGuard strict = new UrlGuard(false);
    private final UrlGuard lenient = new UrlGuard(true);

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd", "ftp://example.com", "gopher://example.com",
            "http://user:pass@example.com",
            "http://127.0.0.1:8080", "http://[::1]/", "http://10.0.0.5",
            "http://172.16.0.1", "http://192.168.1.1",
            "http://169.254.169.254/latest/meta-data",
            "http://0.0.0.0", "http://100.64.0.1", "http://[fd00::1]/",
            "http://[::ffff:127.0.0.1]/", "http://2130706433/", "not a url"
    })
    void strictModeRejectsUnsafeUrls(String url) {
        assertThatThrownBy(() -> strict.validate(url)).isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void strictModeAllowsPublicAddress() {
        assertThatCode(() -> strict.validate("https://8.8.8.8/")).doesNotThrowAnyException();
    }

    @Test
    void lenientModeAllowsLocalhostButStillBlocksBadSchemes() {
        assertThatCode(() -> lenient.validate("http://127.0.0.1:8080")).doesNotThrowAnyException();
        assertThatThrownBy(() -> lenient.validate("file:///etc/passwd"))
                .isInstanceOf(UnsafeUrlException.class);
    }
}
