package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.testgen.RequestData;

import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Two tests with the same fingerprint send the same request, whatever they are called. */
public final class TestFingerprint {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestFingerprint() {}

    public static String of(String method, String path, RequestData data, boolean includeAuth) {
        JsonNode tree = MAPPER.valueToTree(data);
        return method.toUpperCase(Locale.ROOT) + " " + path + "|" + canonical(tree) + "|" + includeAuth;
    }

    // Object keys are sorted recursively, so {"a":1,"b":2} equals {"b":2,"a":1}
    private static String canonical(JsonNode n) {
        if (n.isObject()) {
            TreeMap<String, String> sorted = new TreeMap<>();
            n.fields().forEachRemaining(e -> sorted.put(e.getKey(), canonical(e.getValue())));
            return sorted.entrySet().stream()
                    .map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (n.isArray()) {
            return StreamSupport.stream(n.spliterator(), false)
                    .map(TestFingerprint::canonical)
                    .collect(Collectors.joining(",", "[", "]"));
        }
        return n.toString();
    }
}