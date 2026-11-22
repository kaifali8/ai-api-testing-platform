package com.kaif.apitestplatform.ai;

import com.kaif.apitestplatform.testgen.RequestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestFingerprintTest {

    private RequestData query(String k1, Object v1, String k2, Object v2) {
        RequestData d = new RequestData();
        d.getQueryParams().put(k1, v1);
        d.getQueryParams().put(k2, v2);
        return d;
    }

    @Test
    void keyOrderDoesNotMatter() {
        assertThat(TestFingerprint.of("GET", "/a", query("x", 1, "y", 2), true))
                .isEqualTo(TestFingerprint.of("GET", "/a", query("y", 2, "x", 1), true));
    }

    @Test
    void valuesEndpointAndAuthMatter() {
        String base = TestFingerprint.of("GET", "/a", query("x", 1, "y", 2), true);
        assertThat(base).isNotEqualTo(TestFingerprint.of("GET", "/a", query("x", 1, "y", 3), true));
        assertThat(base).isNotEqualTo(TestFingerprint.of("GET", "/b", query("x", 1, "y", 2), true));
        assertThat(base).isNotEqualTo(TestFingerprint.of("POST", "/a", query("x", 1, "y", 2), true));
        assertThat(base).isNotEqualTo(TestFingerprint.of("GET", "/a", query("x", 1, "y", 2), false));
    }

    @Test
    void nestedBodiesAreCompared() {
        RequestData a = new RequestData();
        a.setBody(java.util.Map.of("guest", java.util.Map.of("age", 5, "name", "A")));
        RequestData b = new RequestData();
        b.setBody(java.util.Map.of("guest", java.util.Map.of("name", "A", "age", 5)));
        assertThat(TestFingerprint.of("POST", "/x", a, false)).isEqualTo(TestFingerprint.of("POST", "/x", b, false));
    }
}