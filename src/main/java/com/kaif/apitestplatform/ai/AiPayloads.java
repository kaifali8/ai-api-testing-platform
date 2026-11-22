package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.enums.Confidence;
import com.kaif.apitestplatform.enums.Severity;
import com.kaif.apitestplatform.exception.AiServiceException;
import org.springframework.http.HttpStatus;

import java.util.*;

public final class AiPayloads {

    static final int MAX_BODY = 3000;
    static final int MAX_TEXT = 2000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiPayloads() {}

    public record Parsed(String likelyCause, String evidence, Severity severity, Confidence confidence,
                         String expectedBehavior, String actualBehavior, String remediation, String model) {}

    /** What we send. Request HEADERS are deliberately not included (they held credentials). */
    public static Map<String, Object> buildRequest(TestResult r, String endpointSummary) {
        List<String> reasons = new ArrayList<>();
        if (r.getFailureReasons() != null) {
            for (String line : r.getFailureReasons().split("\n")) {
                if (reasons.size() < 10) reasons.add(cut(line, 500));
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method", r.getHttpMethod());
        m.put("path", r.getPath());
        m.put("endpoint_summary", cut(endpointSummary, 500));
        m.put("test_name", cut(r.getTestName(), 300));
        m.put("category", r.getCategory().name());
        m.put("expected_statuses", r.getExpectedStatuses());
        m.put("actual_status", r.getActualStatus());
        m.put("failure_reasons", reasons);
        m.put("request_url", cut(r.getRequestUrl(), 2000));
        m.put("request_body", cut(r.getRequestBody(), MAX_BODY));
        m.put("response_body", cut(r.getResponseBody(), MAX_BODY));
        m.put("response_time_ms", r.getResponseTimeMs());
        return m;
    }

    /** Never trust model output: check every field before it reaches the database. */
    public static Parsed parse(String json) {
        if (json == null) throw unusable();
        JsonNode n;
        try {
            n = MAPPER.readTree(json);
        } catch (Exception e) {
            throw unusable();
        }
        if (n == null || !n.isObject()) throw unusable();

        return new Parsed(
                required(n, "likely_cause"),
                required(n, "evidence"),
                enumOf(n, "severity", Severity.class),
                enumOf(n, "confidence", Confidence.class),
                optional(n, "expected_behavior", MAX_TEXT),
                optional(n, "actual_behavior", MAX_TEXT),
                optional(n, "remediation", MAX_TEXT),
                optional(n, "model", 100));
    }

    // ---------- helpers ----------

    private static String required(JsonNode n, String field) {
        String v = optional(n, field, MAX_TEXT);
        if (v == null || v.isBlank()) throw unusable();
        return v;
    }

    private static String optional(JsonNode n, String field, int max) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : cut(v.asText(), max);
    }

    private static <E extends Enum<E>> E enumOf(JsonNode n, String field, Class<E> type) {
        String v = optional(n, field, 50);
        if (v == null) throw unusable();
        try {
            return Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw unusable();
        }
    }

    private static AiServiceException unusable() {
        return new AiServiceException("AI service returned an unusable response", HttpStatus.BAD_GATEWAY);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}