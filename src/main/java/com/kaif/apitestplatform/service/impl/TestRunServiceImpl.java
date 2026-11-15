package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.dto.response.ExecutionResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.TestResultResponse;
import com.kaif.apitestplatform.entity.Project;
import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.entity.TestSuite;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.exception.BadRequestException;
import com.kaif.apitestplatform.exception.ConflictException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.execution.RunExecutor;
import com.kaif.apitestplatform.repository.*;
import com.kaif.apitestplatform.security.UrlGuard;
import com.kaif.apitestplatform.service.TestRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestRunServiceImpl implements TestRunService {

    private static final List<RunStatus> ACTIVE = List.of(RunStatus.QUEUED, RunStatus.RUNNING);

    private final ProjectRepository projectRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectCredentialRepository credentialRepository;
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final UrlGuard urlGuard;
    private final RunExecutor runExecutor;

    @Value("${app.execution.max-tests-per-run:2000}")
    private int maxTestsPerRun;

    @Override
    public ExecutionResponse start(Long projectId, Long suiteId, User owner) {
        Project project = findOwnedProject(projectId, owner);
        TestSuite suite = testSuiteRepository.findByIdAndProjectId(suiteId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Test suite not found with id: " + suiteId));

        long total = testCaseRepository.countBySuiteIdAndEnabledTrue(suiteId);
        if (total == 0) {
            throw new BadRequestException("This suite has no enabled tests");
        }
        if (total > maxTestsPerRun) {
            throw new BadRequestException("Suite has " + total + " enabled tests; the limit per run is " + maxTestsPerRun);
        }

        urlGuard.validate(project.getBaseUrl());   // fail fast with a clear message

        if (executionRepository.existsByProjectIdAndStatusIn(projectId, ACTIVE)) {
            throw new ConflictException("A test run is already in progress for this project");
        }

        String note = null;
        long authTests = testCaseRepository.countBySuiteIdAndEnabledTrueAndIncludeAuthTrue(suiteId);
        if (authTests > 0 && credentialRepository.findByProjectId(projectId).isEmpty()) {
            note = authTests + " test(s) expect authentication but no credentials are configured for this project";
        }

        // save() commits on return, so the worker thread will see the row
        TestExecution exec = executionRepository.save(TestExecution.builder()
                .projectId(projectId)
                .suiteId(suiteId)
                .suiteName(suite.getName())
                .baseUrl(project.getBaseUrl())
                .triggeredBy(owner.getId())
                .status(RunStatus.QUEUED)
                .totalTests((int) total)
                .note(note)
                .build());

        try {
            runExecutor.execute(exec.getId());
        } catch (TaskRejectedException e) {
            exec.setStatus(RunStatus.FAILED);
            exec.setNote("Rejected: the server is busy with other test runs");
            exec.setFinishedAt(LocalDateTime.now());
            executionRepository.save(exec);
            throw new ConflictException("The server is busy with other test runs. Try again shortly");
        }
        return ExecutionResponse.from(exec);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExecutionResponse> list(Long projectId, User owner, Pageable pageable) {
        findOwnedProject(projectId, owner);
        return PageResponse.from(executionRepository.findByProjectId(projectId, pageable)
                .map(ExecutionResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse get(Long projectId, Long executionId, User owner) {
        findOwnedProject(projectId, owner);
        return ExecutionResponse.from(findExecution(projectId, executionId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TestResultResponse> results(Long projectId, Long executionId,
                                                    ResultStatus status, User owner, Pageable pageable) {
        findOwnedProject(projectId, owner);
        findExecution(projectId, executionId);

        Page<TestResult> page = status == null
                ? resultRepository.findByExecutionId(executionId, pageable)
                : resultRepository.findByExecutionIdAndStatus(executionId, status, pageable);
        return PageResponse.from(page.map(TestResultResponse::from));
    }

    // ---------- helpers ----------

    private Project findOwnedProject(Long projectId, User owner) {
        return projectRepository.findByIdAndOwnerId(projectId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private TestExecution findExecution(Long projectId, Long executionId) {
        return executionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Run not found with id: " + executionId));
    }
}