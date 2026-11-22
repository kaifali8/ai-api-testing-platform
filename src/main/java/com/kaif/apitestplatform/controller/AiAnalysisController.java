package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.response.AiAnalysisResponse;
import com.kaif.apitestplatform.dto.response.AnalysisBatchResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/runs/{runId}")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService service;

    @PostMapping("/results/{resultId}/analysis")
    public ResponseEntity<AiAnalysisResponse> analyze(
            @PathVariable Long projectId, @PathVariable Long runId, @PathVariable Long resultId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.analyze(projectId, runId, resultId, user));
    }

    @GetMapping("/results/{resultId}/analysis")
    public ResponseEntity<AiAnalysisResponse> get(
            @PathVariable Long projectId, @PathVariable Long runId, @PathVariable Long resultId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.get(projectId, runId, resultId, user));
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisBatchResponse> analyzeRun(
            @PathVariable Long projectId, @PathVariable Long runId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.analyzeRun(projectId, runId, user));
    }

    @GetMapping("/analyses")
    public ResponseEntity<PageResponse<AiAnalysisResponse>> list(
            @PathVariable Long projectId, @PathVariable Long runId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.list(projectId, runId, user, pageable));
    }
}