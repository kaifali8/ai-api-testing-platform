package com.kaif.apitestplatform.execution;

import com.kaif.apitestplatform.enums.AuthType;
import com.kaif.apitestplatform.testgen.RequestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestRequestBuilderTest {

    private static final String BASE = "https://api.example.com/v1";
    private final TestRequestBuilder builder = new TestRequestBuilder();

    private BuiltRequest build(RequestData d, boolean auth, ResolvedCredential c) {
        return builder.build(BASE, "get", "/pet/{petId}", d, auth, c);
    }

    private RequestData withPetId(Object v) {
        RequestData d = new RequestData();
        d.getPathParams().put("petId", v);
        return d;
    }

    @Test
    void pathValuesAreEncodedSoTheyCannotAddSegments() {
        BuiltRequest r = build(withPetId("a/b c"), false, null);
        assertThat(r.uri().getRawPath()).isEqualTo("/v1/pet/a%2Fb%20c");
    }

    @Test
    void dotSegmentsCannotEscapeThePath() {
        BuiltRequest r = build(withPetId(".."), false, null);
        assertThat(r.uri().getRawPath()).isEqualTo("/v1/pet/%2E%2E");
    }

    @Test
    void missingPathParameterIsRejected() {
        assertThatThrownBy(() -> build(new RequestData(), false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("petId");
    }

    @Test
    void queryParamsAreEncodedAndArraysRepeat() {
        RequestData d = withPetId(1);
        d.getQueryParams().put("tags", List.of("a", "b"));
        d.getQueryParams().put("q", "x y");
        assertThat(build(d, false, null).uri().getRawQuery()).isEqualTo("tags=a&tags=b&q=x+y");
    }

    @Test
    void credentialIsSentButMaskedInTheStoredCopy() {
        ResolvedCredential cred = new ResolvedCredential(AuthType.BEARER, null, null, "tok_123456");
        BuiltRequest r = build(withPetId(1), true, cred);
        assertThat(r.headers().get("Authorization")).isEqualTo("Bearer tok_123456");
        assertThat(r.maskedHeaders().get("Authorization")).isEqualTo("***");
    }

    @Test
    void credentialIsOmittedWhenTestSaysNoAuth() {
        ResolvedCredential cred = new ResolvedCredential(AuthType.BEARER, null, null, "tok_123456");
        assertThat(build(withPetId(1), false, cred).headers()).doesNotContainKey("Authorization");
    }

    @Test
    void forbiddenAndInjectedHeadersAreRejected() {
        RequestData host = withPetId(1);
        host.getHeaders().put("Host", "evil.com");
        assertThatThrownBy(() -> build(host, false, null)).isInstanceOf(IllegalArgumentException.class);

        RequestData injected = withPetId(1);
        injected.getHeaders().put("X-Test", "a\r\nX-Evil: 1");
        assertThatThrownBy(() -> build(injected, false, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void credentialToStringNeverShowsTheSecret() {
        ResolvedCredential cred = new ResolvedCredential(AuthType.BEARER, null, null, "tok_123456");
        assertThat(cred.toString()).doesNotContain("tok_123456");
    }
}