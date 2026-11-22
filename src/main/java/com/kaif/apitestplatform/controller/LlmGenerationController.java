package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.response.LlmGenerationJobResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.service.LlmGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class LlmGenerationController {

    private final LlmGenerationService service;

    @PostMapping("/test-suites/{suiteId}/generate-llm")
    public ResponseEntity<LlmGenerationJobResponse> start(
            @PathVariable Long projectId, @PathVariable Long suiteId,
            @RequestParam(defaultValue = "5") int maxPerEndpoint,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(service.start(projectId, suiteId, maxPerEndpoint, user));
    }

    @GetMapping("/llm-generations")
    public ResponseEntity<PageResponse<LlmGenerationJobResponse>> list(
            @PathVariable Long projectId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.list(projectId, user, pageable));
    }

    @GetMapping("/llm-generations/{jobId}")
    public ResponseEntity<LlmGenerationJobResponse> get(
            @PathVariable Long projectId, @PathVariable Long jobId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.get(projectId, jobId, user));
    }
}