package com.kaif.apitestplatform.execution;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseValidatorTest {

    private final ResponseValidator validator = new ResponseValidator(5000);

    private static final String SCHEMAS =
            "{\"200\":{\"type\":\"object\",\"required\":[\"id\"],"
                    + "\"properties\":{\"id\":{\"type\":\"integer\"}}},\"404\":null}";

    private HttpResponseData json(int status, String body, long ms) {
        return new HttpResponseData(status, Map.of("content-type", "application/json"), body, false, ms);
    }

    @Test
    void matchingStatusAndSchemaPasses() {
        assertThat(validator.validate(Set.of(200), json(200, "{\"id\":1}", 50), SCHEMAS)).isEmpty();
    }

    @Test
    void serverErrorWhereClientErrorExpectedFailsWithHint() {
        List<String> failures = validator.validate(
                ResponseValidator.parseStatuses("400,422"), json(500, "{}", 50), null);
        assertThat(failures).singleElement().satisfies(m ->
                assertThat(m).contains("400 or 422").contains("500").contains("server error"));
    }

    @Test
    void slowResponseFails() {
        assertThat(validator.validate(Set.of(200), json(200, "{\"id\":1}", 6000), SCHEMAS))
                .anyMatch(m -> m.contains("exceeded"));
    }

    @Test
    void wrongFieldTypeFailsSchemaCheck() {
        assertThat(validator.validate(Set.of(200), json(200, "{\"id\":\"abc\"}", 50), SCHEMAS))
                .anyMatch(m -> m.startsWith("Schema mismatch"));
    }

    @Test
    void missingRequiredFieldFailsSchemaCheck() {
        assertThat(validator.validate(Set.of(200), json(200, "{}", 50), SCHEMAS))
                .anyMatch(m -> m.startsWith("Schema mismatch"));
    }

    @Test
    void schemaCheckIsSkippedForTruncatedBodies() {
        HttpResponseData r = new HttpResponseData(200, Map.of("content-type", "application/json"),
                "{\"id\":\"abc\"", true, 10);
        assertThat(validator.validate(Set.of(200), r, SCHEMAS)).isEmpty();
    }

    @Test
    void schemaCheckIsSkippedWhenSpecDefinesNoBody() {
        assertThat(validator.validate(Set.of(404), json(404, "anything", 10), SCHEMAS)).isEmpty();
    }

    @Test
    void schemaCheckIsSkippedForNonJsonResponses() {
        HttpResponseData r = new HttpResponseData(200, Map.of("content-type", "text/html"), "<html>", false, 10);
        assertThat(validator.validate(Set.of(200), r, SCHEMAS)).isEmpty();
    }
}