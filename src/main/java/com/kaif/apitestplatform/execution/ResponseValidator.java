package com.kaif.apitestplatform.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** Decides PASS or FAIL. Returns the list of reasons; an empty list means PASS. */
@Component
public class ResponseValidator {

    private static final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

    private final ObjectMapper mapper = new ObjectMapper();
    private final long maxResponseTimeMs;

    public ResponseValidator(@Value("${app.execution.max-response-time-ms:5000}") long maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public static Set<Integer> parseStatuses(String csv) {
        Set<Integer> out = new LinkedHashSet<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) out.add(Integer.parseInt(t));
            }
        }
        return out;
    }

    public List<String> validate(Set<Integer> expected, HttpResponseData response, String responseSchemasJson) {
        List<String> failures = new ArrayList<>();

        // 1. Status code
        if (!expected.contains(response.status())) {
            String msg = "Expected status " + expected.stream().map(String::valueOf)
                    .collect(Collectors.joining(" or ")) + " but got " + response.status();
            if (response.status() >= 500 && expected.stream().allMatch(c -> c < 500)) {
                msg += " (server error: the API likely failed to handle this input gracefully)";
            }
            failures.add(msg);
        }

        // 2. Response time
        if (response.timeMs() > maxResponseTimeMs) {
            failures.add("Response time " + response.timeMs() + " ms exceeded the limit of "
                    + maxResponseTimeMs + " ms");
        }

        // 3. Response schema
        validateSchema(response, responseSchemasJson, failures);

        return failures;
    }

    private void validateSchema(HttpResponseData r, String schemasJson, List<String> failures) {
        // Cannot judge a body we only partly read
        if (schemasJson == null || schemasJson.isBlank() || r.truncated()) return;

        JsonNode schemas;
        try {
            schemas = mapper.readTree(schemasJson);
        } catch (JsonProcessingException e) {
            return;
        }

        String code = String.valueOf(r.status());
        JsonNode schemaNode = schemas.has(code) ? schemas.get(code) : schemas.get("default");
        if (schemaNode == null || schemaNode.isNull()) return;   // spec defines no body for this status

        String contentType = r.headers().getOrDefault("content-type", "");
        if (!contentType.isEmpty() && !contentType.toLowerCase(Locale.ROOT).contains("json")) return;

        if (r.body() == null || r.body().isBlank()) {
            if (r.status() >= 200 && r.status() < 300) {
                failures.add("Response body is empty but the specification defines a response schema");
            }
            return;
        }

        JsonNode bodyNode;
        try {
            bodyNode = mapper.readTree(r.body());
        } catch (JsonProcessingException e) {
            failures.add("Response body is not valid JSON");
            return;
        }

        Set<ValidationMessage> errors;
        try {
            errors = SCHEMA_FACTORY.getSchema(schemaNode).validate(bodyNode);
        } catch (Exception e) {
            return;   // a schema we cannot interpret: skip rather than guess
        }
        errors.stream().map(ValidationMessage::getMessage).sorted().limit(5)
                .forEach(m -> failures.add("Schema mismatch: " + m));
    }
}