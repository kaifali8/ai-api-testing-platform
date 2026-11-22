package com.kaif.apitestplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.kaif.apitestplatform.entity.LlmGenerationJob;
import com.kaif.apitestplatform.enums.RunStatus;

import java.time.LocalDateTime;

public record LlmGenerationJobResponse(
        Long id, Long suiteId, RunStatus status, int maxPerEndpoint,
        int endpointsTotal, int endpointsProcessed, int endpointsFailed,
        int proposed, int accepted, int duplicates, int invalid, double acceptanceRate,
        @JsonRawValue String rejectionReasons,      // emitted as a real JSON object
        String model, String note,
        LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime finishedAt
) {
    public static LlmGenerationJobResponse from(LlmGenerationJob j) {
        double rate = j.getProposed() == 0 ? 0.0 : Math.round(j.getAccepted() * 1000.0 / j.getProposed()) / 10.0;
        return new LlmGenerationJobResponse(
                j.getId(), j.getSuiteId(), j.getStatus(), j.getMaxPerEndpoint(),
                j.getEndpointsTotal(), j.getEndpointsProcessed(), j.getEndpointsFailed(),
                j.getProposed(), j.getAccepted(), j.getDuplicates(), j.getInvalid(), rate,
                j.getRejectionReasons() == null ? "{}" : j.getRejectionReasons(),
                j.getModel(), j.getNote(),
                j.getCreatedAt(), j.getStartedAt(), j.getFinishedAt());
    }
}