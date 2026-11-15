package com.kaif.apitestplatform.dto.response;

import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.enums.RunStatus;

import java.time.LocalDateTime;

public record ExecutionResponse(
        Long id, Long suiteId, String suiteName, RunStatus status,
        int totalTests, int passed, int failed, int errored, double passRate,
        String baseUrl, String note,
        LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime finishedAt
) {
    public static ExecutionResponse from(TestExecution e) {
        int done = e.getPassed() + e.getFailed() + e.getErrored();
        double passRate = done == 0 ? 0.0 : Math.round(e.getPassed() * 1000.0 / done) / 10.0;
        return new ExecutionResponse(
                e.getId(), e.getSuiteId(), e.getSuiteName(), e.getStatus(),
                e.getTotalTests(), e.getPassed(), e.getFailed(), e.getErrored(), passRate,
                e.getBaseUrl(), e.getNote(),
                e.getCreatedAt(), e.getStartedAt(), e.getFinishedAt());
    }
}