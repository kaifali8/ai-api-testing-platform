package com.kaif.apitestplatform.dto.response;

import com.kaif.apitestplatform.entity.AiAnalysis;
import com.kaif.apitestplatform.enums.Confidence;
import com.kaif.apitestplatform.enums.Severity;

import java.time.LocalDateTime;

public record AiAnalysisResponse(
        Long id, Long resultId, String likelyCause, String evidence,
        Severity severity, Confidence confidence,
        String expectedBehavior, String actualBehavior, String remediation,
        String model, LocalDateTime createdAt
) {
    public static AiAnalysisResponse from(AiAnalysis a) {
        return new AiAnalysisResponse(
                a.getId(), a.getResultId(), a.getLikelyCause(), a.getEvidence(),
                a.getSeverity(), a.getConfidence(),
                a.getExpectedBehavior(), a.getActualBehavior(), a.getRemediation(),
                a.getModel(), a.getCreatedAt());
    }
}