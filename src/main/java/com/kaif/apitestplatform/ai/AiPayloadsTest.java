package com.kaif.apitestplatform.ai;

import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.enums.Severity;
import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.exception.AiServiceException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiPayloadsTest {

    private TestResult failedResult() {
        return TestResult.builder()
                .id(1L).httpMethod("POST").path("/pet").testName("Missing name")
                .category(TestCategory.NEGATIVE).expectedStatuses("400,422").actualStatus(500)
                .failureReasons("Expected status 400 or 422 but got 500\nSecond reason")
                .requestUrl("https://api.example.com/pet")
                .requestHeaders("{\"Authorization\":\"***\"}")
                .requestBody("{}")
                .responseBody("x".repeat(10_000))
                .responseTimeMs(120L)
                .build();
    }

    @Test
    void requestNeverContainsHeaders() {
        Map<String, Object> req = AiPayloads.buildRequest(failedResult(), "Add a pet");
        assertThat(req).doesNotContainKey("request_headers");
        assertThat(req.toString()).doesNotContain("Authorization");
    }

    @Test
    void largeBodiesAreTruncated() {
        Map<String, Object> req = AiPayloads.buildRequest(failedResult(), null);
        assertThat(((String) req.get("response_body")).length()).isEqualTo(AiPayloads.MAX_BODY);
    }

    @Test
    void parsesValidResponse() {
        String json = "{\"likely_cause\":\"Missing validation\",\"evidence\":\"500 on empty body\","
                + "\"severity\":\"high\",\"confidence\":\"MEDIUM\",\"expected_behavior\":\"400\","
                + "\"actual_behavior\":\"500\",\"remediation\":\"Validate input\",\"model\":\"m\"}";
        AiPayloads.Parsed p = AiPayloads.parse(json);
        assertThat(p.severity()).isEqualTo(Severity.HIGH);   // case-insensitive
        assertThat(p.likelyCause()).isEqualTo("Missing validation");
    }

    @Test
    void rejectsUnknownSeverityMissingFieldsAndGarbage() {
        String badSeverity = "{\"likely_cause\":\"a\",\"evidence\":\"b\",\"severity\":\"EXTREME\",\"confidence\":\"LOW\"}";
        String missingEvidence = "{\"likely_cause\":\"a\",\"severity\":\"LOW\",\"confidence\":\"LOW\"}";
        assertThatThrownBy(() -> AiPayloads.parse(badSeverity)).isInstanceOf(AiServiceException.class);
        assertThatThrownBy(() -> AiPayloads.parse(missingEvidence)).isInstanceOf(AiServiceException.class);
        assertThatThrownBy(() -> AiPayloads.parse("not json")).isInstanceOf(AiServiceException.class);
        assertThatThrownBy(() -> AiPayloads.parse(null)).isInstanceOf(AiServiceException.class);
    }
}