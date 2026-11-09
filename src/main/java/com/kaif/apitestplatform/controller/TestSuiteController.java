package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.response.GenerationSummaryResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.TestCaseResponse;
import com.kaif.apitestplatform.dto.response.TestSuiteResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.service.TestGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/test-suites")
@RequiredArgsConstructor
public class TestSuiteController {

    private final TestGenerationService service;

    @PostMapping("/generate")
    public ResponseEntity<GenerationSummaryResponse> generate(
            @PathVariable Long projectId,
            @RequestParam(required = false) String suiteName,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.generate(projectId, suiteName, user));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TestSuiteResponse>> listSuites(
            @PathVariable Long projectId,
            @PageableDefault(size = 10,sort = "createdAt",direction = Sort.Direction.DESC)Pageable pageable,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.ok(service.listSuites(projectId,user,pageable));
    }

    @GetMapping("/{suiteId}/tests")
    public ResponseEntity<PageResponse<TestCaseResponse>> listTests(
            @PathVariable Long projectId,
            @PathVariable Long suiteId,
            @RequestParam(required = false)TestCategory category,
            @PageableDefault(size = 20,sort = "id") Pageable pageable,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.ok(service.listTests(projectId,suiteId,category,user,pageable));
    }

}
