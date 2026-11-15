package com.kaif.apitestplatform.execution;

import java.net.URI;
import java.util.Map;

/** A fully prepared request. `headers` contains real credentials: send it, never store it. */
public record BuiltRequest(
        String method,
        URI uri,
        Map<String, String> headers,
        Map<String, String> maskedHeaders,   // safe to store and show
        String body
) {}