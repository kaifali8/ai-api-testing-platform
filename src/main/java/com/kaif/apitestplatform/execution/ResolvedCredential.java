package com.kaif.apitestplatform.execution;

import com.kaif.apitestplatform.enums.AuthType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Decrypted credential, held in memory only for the duration of a run. */
public record ResolvedCredential(AuthType type, String headerName, String username, String secret) {

    public String name() {
        return type == AuthType.API_KEY ? headerName : "Authorization";
    }

    public String value() {
        return switch (type) {
            case BEARER -> "Bearer " + secret;
            case API_KEY -> secret;
            case BASIC -> "Basic " + Base64.getEncoder()
                    .encodeToString((username + ":" + secret).getBytes(StandardCharsets.UTF_8));
        };
    }

    // Records auto-generate toString() with every field. Override it so the secret can never be logged.
    @Override
    public String toString() {
        return "ResolvedCredential[type=" + type + ", secret=***]";
    }
}