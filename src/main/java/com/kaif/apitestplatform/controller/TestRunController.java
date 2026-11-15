package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.response.ExecutionResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.TestResultResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.service.TestRunService;
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
public class TestRunController {

    private final TestRunService service;

    // 202 Accepted: the work has been queued, not finished
    @PostMapping("/test-suites/{suiteId}/runs")
    public ResponseEntity<ExecutionResponse> start(
            @PathVariable Long projectId, @PathVariable Long suiteId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.start(projectId, suiteId, user));
    }

    @GetMapping("/runs")
    public ResponseEntity<PageResponse<ExecutionResponse>> list(
            @PathVariable Long projectId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.list(projectId, user, pageable));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ExecutionResponse> get(
            @PathVariable Long projectId, @PathVariable Long runId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.get(projectId, runId, user));
    }

    @GetMapping("/runs/{runId}/results")
    public ResponseEntity<PageResponse<TestResultResponse>> results(
            @PathVariable Long projectId, @PathVariable Long runId,
            @RequestParam(required = false) ResultStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.results(projectId, runId, status, user, pageable));
    }
}