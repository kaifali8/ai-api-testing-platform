package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.ApiParameter;
import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.testgen.RequestData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Decides whether ONE proposed test is acceptable. Pure logic: no Spring, no database, no network.
 * Every rejection has a stable reason code, which feeds the research metrics.
 */
public final class LlmTestValidator {

    public record Accepted(String name, String description, TestCategory category, RequestData data,
                           String requestDataJson, String expectedStatuses, boolean includeAuth) {}

    public record Outcome(Accepted accepted, String rejection) {
        public boolean isAccepted() {
            return accepted != null;
        }
    }

    // Control flow only: no stack trace needed
    private static final class Reject extends RuntimeException {
        Reject(String reason) {
            super(reason, null, false, false);
        }
    }

    private static final Set<TestCategory> ALLOWED = EnumSet.of(
            TestCategory.NEGATIVE, TestCategory.BOUNDARY, TestCategory.VALIDATION,
            TestCategory.AUTHENTICATION, TestCategory.BUSINESS_LOGIC);

    private static final int MAX_VALUE_CHARS = 5_000;
    private static final int MAX_JSON_FIELD_CHARS = 10_000;
    private static final int MAX_TOTAL_CHARS = 20_000;
    private static final int MAX_ARRAY_ITEMS = 20;

    private final ObjectMapper mapper = new ObjectMapper();

    public Outcome validate(ApiDefinition api, JsonNode test) {
        try {
            return new Outcome(check(api, test), null);
        } catch (Reject r) {
            return new Outcome(null, r.getMessage());
        }
    }

    private Accepted check(ApiDefinition api, JsonNode t) {
        if (t == null || !t.isObject()) throw new Reject("malformed");

        String name = text(t, "name");
        if (name == null || name.isBlank()) throw new Reject("bad_name");
        name = cut(name.trim(), 300);

        TestCategory category = category(t);
        String expected = expectedStatuses(category, statuses(t));

        Map<String, String> pathDeclared = declared(api, "path", false);
        RequestData data = new RequestData();
        data.setPathParams(params(t, "path_params_json", pathDeclared, false, "unknown_path_param", false));
        data.setQueryParams(params(t, "query_params_json", declared(api, "query", false), false,
                "unknown_query_param", true));
        data.setHeaders(params(t, "headers_json", declared(api, "header", true), true,
                "unknown_header", false));

        // A request with an unfilled path placeholder cannot even be built
        for (String required : pathDeclared.values()) {
            if (!data.getPathParams().containsKey(required)) throw new Reject("missing_path_param");
        }
        for (Object v : data.getHeaders().values()) {
            String s = String.valueOf(v);
            if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0) throw new Reject("header_injection");
        }
        data.setBody(body(api, t));

        String json;
        try {
            json = mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new Reject("malformed");
        }
        if (json.length() > MAX_TOTAL_CHARS) throw new Reject("too_large");

        // The model never decides authentication: it follows the endpoint
        boolean includeAuth;
        if (category == TestCategory.AUTHENTICATION) {
            if (!api.isRequiresAuth()) throw new Reject("auth_test_on_public_endpoint");
            includeAuth = false;
        } else {
            includeAuth = api.isRequiresAuth();
        }

        return new Accepted(name, description(t), category, data, json, expected, includeAuth);
    }

    // ---------- status codes ----------

    private TreeSet<Integer> statuses(JsonNode t) {
        JsonNode arr = t.get("expected_statuses");
        if (arr == null || !arr.isArray() || arr.isEmpty() || arr.size() > 5) throw new Reject("bad_status");
        TreeSet<Integer> set = new TreeSet<>();
        for (JsonNode n : arr) {
            if (!n.isIntegralNumber() || !n.canConvertToInt()) throw new Reject("bad_status");
            set.add(n.intValue());
        }
        boolean all2xx = set.stream().allMatch(c -> c >= 200 && c <= 299);
        boolean all4xx = set.stream().allMatch(c -> c >= 400 && c <= 499);
        if (!all2xx && !all4xx) throw new Reject("bad_status");   // no 5xx, no 3xx, no mixing
        return set;
    }

    private String expectedStatuses(TestCategory category, TreeSet<Integer> s) {
        boolean clientError = s.first() >= 400;
        if (category == TestCategory.NEGATIVE && !clientError) throw new Reject("status_category_mismatch");
        if (category == TestCategory.AUTHENTICATION && !Set.of(401, 403).containsAll(s)) {
            throw new Reject("status_category_mismatch");
        }
        TreeSet<Integer> out = new TreeSet<>(s);
        // Same reasoning as the rule generator: APIs disagree between 400 and 422
        if (clientError && category != TestCategory.AUTHENTICATION && (out.contains(400) || out.contains(422))) {
            out.add(400);
            out.add(422);
        }
        return out.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    // ---------- parameters and body ----------

    private Map<String, String> declared(ApiDefinition api, String location, boolean caseInsensitive) {
        Map<String, String> m = new LinkedHashMap<>();
        for (ApiParameter p : api.getParameters()) {
            if (location.equalsIgnoreCase(p.getLocation())) {
                m.put(caseInsensitive ? p.getName().toLowerCase(Locale.ROOT) : p.getName(), p.getName());
            }
        }
        return m;
    }

    private Map<String, Object> params(JsonNode t, String field, Map<String, String> declared,
                                       boolean caseInsensitive, String unknownReason, boolean allowArrays) {
        Map<String, Object> out = new LinkedHashMap<>();
        JsonNode obj = jsonField(t, field, "invalid_params_json");
        if (obj == null) return out;
        if (!obj.isObject()) throw new Reject("invalid_params_json");

        Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = caseInsensitive ? e.getKey().toLowerCase(Locale.ROOT) : e.getKey();
            String actual = declared.get(key);
            if (actual == null) throw new Reject(unknownReason);   // hallucinated parameter
            Object v = value(e.getValue(), allowArrays);
            if (v != null) out.put(actual, v);
        }
        return out;
    }

    private Object body(ApiDefinition api, JsonNode t) {
        JsonNode b = jsonField(t, "body_json", "invalid_body_json");
        if (b == null || b.isNull()) return null;
        if (api.getRequestBodySchema() == null) throw new Reject("body_not_allowed");
        return mapper.convertValue(b, Object.class);
    }

    // Accepts JSON text (what we asked for), and tolerates a raw object or array
    private JsonNode jsonField(JsonNode t, String field, String badReason) {
        JsonNode f = t.get(field);
        if (f == null || f.isNull()) return null;
        if (f.isObject() || f.isArray()) return f;
        if (!f.isTextual()) throw new Reject(badReason);
        String s = f.asText().trim();
        if (s.isEmpty()) return null;
        if (s.length() > MAX_JSON_FIELD_CHARS) throw new Reject("too_large");
        try {
            return mapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new Reject(badReason);
        }
    }

    private Object value(JsonNode n, boolean allowArrays) {
        if (n.isArray()) {
            if (!allowArrays || n.size() > MAX_ARRAY_ITEMS) throw new Reject("non_scalar_value");
            List<Object> list = new ArrayList<>();
            for (JsonNode item : n) {
                Object v = scalar(item);
                if (v != null) list.add(v);
            }
            return list;
        }
        return scalar(n);
    }

    private Object scalar(JsonNode n) {
        if (n.isNull()) return null;
        if (n.isTextual()) {
            if (n.asText().length() > MAX_VALUE_CHARS) throw new Reject("value_too_long");
            return n.asText();
        }
        if (n.isBoolean()) return n.asBoolean();
        if (n.isIntegralNumber()) return n.canConvertToLong() ? (Object) n.longValue() : n.bigIntegerValue();
        if (n.isFloatingPointNumber()) return n.doubleValue();
        throw new Reject("non_scalar_value");   // objects and nested arrays are not valid parameter values
    }

    // ---------- small helpers ----------

    private TestCategory category(JsonNode t) {
        String c = text(t, "category");
        if (c == null) throw new Reject("bad_category");
        try {
            TestCategory cat = TestCategory.valueOf(c.trim().toUpperCase(Locale.ROOT));
            if (!ALLOWED.contains(cat)) throw new Reject("bad_category");
            return cat;
        } catch (IllegalArgumentException e) {
            throw new Reject("bad_category");
        }
    }

    private String description(JsonNode t) {
        String d = text(t, "description");
        String r = text(t, "rationale");
        String out = d == null ? "" : d.trim();
        if (r != null && !r.isBlank()) out = (out.isEmpty() ? "" : out + " ") + "Why: " + r.trim();
        return cut(out, 500);
    }

    private static String text(JsonNode t, String field) {
        JsonNode n = t.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}