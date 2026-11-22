package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.ApiParameter;
import com.kaif.apitestplatform.exception.AiServiceException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LlmPayloads {

    static final int MAX_TESTS_READ = 30;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LlmPayloads() {}

    public record Proposals(List<JsonNode> tests, String model) {}

    public static Map<String, Object> buildRequest(ApiDefinition api, String baselineJson,
                                                   List<String> existingNames, int maxTests) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (ApiParameter p : api.getParameters()) {
            if (params.size() >= 50) break;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", cut(p.getName(), 200));
            m.put("location", cut(p.getLocation(), 10));
            m.put("required", p.isRequired());
            m.put("type", cut(p.getType(), 30));
            m.put("format", cut(p.getFormat(), 30));
            m.put("minimum", p.getMinimum());
            m.put("maximum", p.getMaximum());
            m.put("min_length", p.getMinLength());
            m.put("max_length", p.getMaxLength());
            m.put("enum_values", cut(p.getEnumValues(), 1000));
            m.put("description", cut(p.getDescription(), 500));
            params.add(m);
        }

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("method", api.getHttpMethod());
        req.put("path", cut(api.getPath(), 500));
        req.put("summary", cut(api.getSummary(), 500));
        req.put("description", cut(api.getDescription(), 1500));
        req.put("requires_auth", api.isRequiresAuth());
        req.put("parameters", params);
        req.put("request_body_schema", cut(api.getRequestBodySchema(), 6000));
        req.put("response_codes", responseCodes(api.getResponseSchemas()));
        req.put("baseline_request", cut(baselineJson, 4000));
        req.put("existing_test_names", existingNames.stream().limit(60).map(n -> cut(n, 500)).toList());
        req.put("max_tests", maxTests);
        return req;
    }

    /** The response must be an object with a "tests" array. Each test is judged separately later. */
    public static Proposals parse(String json) {
        if (json == null) throw unusable();
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw unusable();
        }
        if (root == null || !root.isObject()) throw unusable();
        JsonNode arr = root.get("tests");
        if (arr == null || !arr.isArray()) throw unusable();

        List<JsonNode> tests = new ArrayList<>();
        for (JsonNode t : arr) {
            if (tests.size() >= MAX_TESTS_READ) break;
            tests.add(t);
        }
        String model = root.hasNonNull("model") ? cut(root.get("model").asText(), 100) : null;
        return new Proposals(tests, model);
    }

    private static List<String> responseCodes(String json) {
        List<String> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            MAPPER.readTree(json).fieldNames().forEachRemaining(c -> {
                if (out.size() < 30) out.add(cut(c, 20));
            });
        } catch (Exception ignored) {
            // no codes is acceptable
        }
        return out;
    }

    private static AiServiceException unusable() {
        return new AiServiceException("AI service returned an unusable response", HttpStatus.BAD_GATEWAY);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}