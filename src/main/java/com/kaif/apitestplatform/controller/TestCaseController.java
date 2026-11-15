package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.request.UpdateTestCaseRequest;
import com.kaif.apitestplatform.dto.response.TestCaseResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.service.TestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/test-suites/{suiteId}/tests/{testId}")
@RequiredArgsConstructor
public class TestCaseController {
    private final TestCaseService service;

    @PatchMapping
    public ResponseEntity<TestCaseResponse> update(
            @PathVariable Long projectId, @PathVariable Long suiteId, @PathVariable Long testId,
            @Valid @RequestBody UpdateTestCaseRequest request,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.ok(service.update(projectId, suiteId, testId, request, user));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId, @PathVariable Long suiteId, @PathVariable Long testId,
            @AuthenticationPrincipal User user
    ){
        service.delete(projectId, suiteId, testId, user);
        return ResponseEntity.noContent().build();
    }
}
