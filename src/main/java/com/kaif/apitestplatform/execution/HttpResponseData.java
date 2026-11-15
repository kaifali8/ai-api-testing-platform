package com.kaif.apitestplatform.execution;

import java.util.Map;

public record HttpResponseData(
        int status,
        Map<String, String> headers,   // lower-case names
        String body,
        boolean truncated,
        long timeMs
) {}