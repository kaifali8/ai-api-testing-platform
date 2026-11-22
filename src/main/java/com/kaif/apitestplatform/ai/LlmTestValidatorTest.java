package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.ApiParameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmTestValidatorTest {

    private static final ObjectMapper M = new ObjectMapper();
    private final LlmTestValidator validator = new LlmTestValidator();

    private ApiDefinition api(boolean auth, boolean withBody) {
        ApiDefinition a = ApiDefinition.builder()
                .path("/pets/{petId}").httpMethod("PUT").requiresAuth(auth)
                .requestBodySchema(withBody ? "{\"type\":\"object\"}" : null).build();
        a.getParameters().add(ApiParameter.builder().name("petId").location("path").required(true).type("integer").build());
        a.getParameters().add(ApiParameter.builder().name("status").location("query").type("string").build());
        a.getParameters().add(ApiParameter.builder().name("X-Trace").location("header").type("string").build());
        return a;
    }

    private JsonNode test(String category, String path, String query, String headers, String body, int... statuses) {
        ObjectNode n = M.createObjectNode();
        n.put("name", "t");
        n.put("description", "d");
        n.put("category", category);
        n.put("path_params_json", path);
        n.put("query_params_json", query);
        n.put("headers_json", headers);
        n.put("body_json", body);
        n.put("rationale", "r");
        ArrayNode arr = n.putArray("expected_statuses");
        for (int s : statuses) arr.add(s);
        return n;
    }

    private static final String PET = "{\"petId\":1}";

    @Test
    void acceptsValidBusinessRuleTestAndFollowsEndpointAuth() {
        var o = validator.validate(api(true, false), test("BUSINESS_LOGIC", "{\"petId\":99999999}", "{}", "{}", "", 404));
        assertThat(o.isAccepted()).isTrue();
        assertThat(o.accepted().includeAuth()).isTrue();
        assertThat(o.accepted().expectedStatuses()).isEqualTo("404");
    }

    @Test
    void widensSingleValidationStatusLikeTheRuleGenerator() {
        var o = validator.validate(api(false, false), test("NEGATIVE", PET, "{\"status\":\"x\"}", "{}", "", 400));
        assertThat(o.accepted().expectedStatuses()).isEqualTo("400,422");
    }

    @Test
    void authenticationTestOmitsCredentials() {
        var o = validator.validate(api(true, false), test("AUTHENTICATION", PET, "{}", "{}", "", 401));
        assertThat(o.accepted().includeAuth()).isFalse();
    }

    @Test
    void rejectsInventedParameters() {
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "{\"hotelId\":1}", "{}", "", 400)).rejection())
                .isEqualTo("unknown_query_param");
        assertThat(validator.validate(api(false, false), test("VALIDATION", "{\"petId\":1,\"x\":2}", "{}", "{}", "", 400)).rejection())
                .isEqualTo("unknown_path_param");
    }

    @Test
    void neverAcceptsServerErrorOrMixedStatusExpectations() {
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "{}", "{}", "", 500)).rejection())
                .isEqualTo("bad_status");
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "{}", "{}", "", 200, 400)).rejection())
                .isEqualTo("bad_status");
    }

    @Test
    void negativeTestCannotExpectSuccess() {
        assertThat(validator.validate(api(false, false), test("NEGATIVE", PET, "{}", "{}", "", 200)).rejection())
                .isEqualTo("status_category_mismatch");
    }

    @Test
    void rejectsMissingPathParameter() {
        assertThat(validator.validate(api(false, false), test("VALIDATION", "{}", "{}", "{}", "", 400)).rejection())
                .isEqualTo("missing_path_param");
    }

    @Test
    void rejectsBodyOnEndpointWithoutOne() {
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "{}", "{}", "{\"a\":1}", 400)).rejection())
                .isEqualTo("body_not_allowed");
        assertThat(validator.validate(api(false, true), test("VALIDATION", PET, "{}", "{}", "{\"a\":1}", 400)).isAccepted())
                .isTrue();
    }

    @Test
    void rejectsAuthenticationTestOnPublicEndpoint() {
        assertThat(validator.validate(api(false, false), test("AUTHENTICATION", PET, "{}", "{}", "", 401)).rejection())
                .isEqualTo("auth_test_on_public_endpoint");
    }

    @Test
    void rejectsNestedObjectsAndHeaderInjection() {
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "{\"status\":{\"a\":1}}", "{}", "", 400)).rejection())
                .isEqualTo("non_scalar_value");
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "{}", "{\"X-Trace\":\"a\\r\\nb\"}", "", 400)).rejection())
                .isEqualTo("header_injection");
    }

    @Test
    void rejectsMalformedInput() {
        assertThat(validator.validate(api(false, false), M.createArrayNode()).rejection()).isEqualTo("malformed");
        assertThat(validator.validate(api(false, false), test("VALIDATION", PET, "not json", "{}", "", 400)).rejection())
                .isEqualTo("invalid_params_json");
        assertThat(validator.validate(api(false, false), test("POSITIVE", PET, "{}", "{}", "", 200)).rejection())
                .isEqualTo("bad_category");
    }
}