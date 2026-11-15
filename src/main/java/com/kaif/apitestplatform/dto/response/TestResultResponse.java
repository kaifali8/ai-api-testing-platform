package com.kaif.apitestplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.enums.TestCategory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record TestResultResponse(
        Long id, Long testCaseId, String testName, TestCategory category,
        String httpMethod, String path, String expectedStatuses,
        ResultStatus status, Integer actualStatus, Long responseTimeMs,
        List<String> failureReasons, String errorMessage,
        String requestUrl,
        @JsonRawValue String requestHeaders,
        String requestBody,
        @JsonRawValue String responseHeaders,
        String responseBody, boolean responseTruncated,
        LocalDateTime executedAt
) {
    public static TestResultResponse from(TestResult r) {
        List<String> reasons = r.getFailureReasons() == null
                ? List.of() : Arrays.asList(r.getFailureReasons().split("\n"));
        return new TestResultResponse(
                r.getId(), r.getTestCaseId(), r.getTestName(), r.getCategory(),
                r.getHttpMethod(), r.getPath(), r.getExpectedStatuses(),
                r.getStatus(), r.getActualStatus(), r.getResponseTimeMs(),
                reasons, r.getErrorMessage(),
                r.getRequestUrl(), r.getRequestHeaders(), r.getRequestBody(),
                r.getResponseHeaders(), r.getResponseBody(), r.isResponseTruncated(),
                r.getExecutedAt());
    }
}