package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.ai.AiAnalysisCore;
import com.kaif.apitestplatform.ai.AiAnalysisWorker;
import com.kaif.apitestplatform.dto.response.AiAnalysisResponse;
import com.kaif.apitestplatform.dto.response.AnalysisBatchResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.exception.BadRequestException;
import com.kaif.apitestplatform.exception.ConflictException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.*;
import com.kaif.apitestplatform.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Not @Transactional at class level: the AI call must not hold a DB connection
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final ProjectRepository projectRepository;
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final AiAnalysisRepository analysisRepository;
    private final AiAnalysisCore core;
    private final AiAnalysisWorker worker;

    @Value("${app.ai.max-batch:25}")
    private int maxBatch;

    @Override
    public AiAnalysisResponse analyze(Long projectId, Long runId, Long resultId, User owner) {
        TestResult r = findOwnedResult(projectId, runId, resultId, owner);
        if (r.getStatus() != ResultStatus.FAILED) {
            throw new BadRequestException("Only FAILED results can be analyzed");
        }
        return AiAnalysisResponse.from(core.analyzeAndStore(r, projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public AiAnalysisResponse get(Long projectId, Long runId, Long resultId, User owner) {
        findOwnedResult(projectId, runId, resultId, owner);
        return analysisRepository.findByResultId(resultId)
                .map(AiAnalysisResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis exists yet for this result"));
    }

    @Override
    public AnalysisBatchResponse analyzeRun(Long projectId, Long runId, User owner) {
        TestExecution exec = findOwnedExecution(projectId, runId, owner);
        if (exec.getStatus() == RunStatus.QUEUED || exec.getStatus() == RunStatus.RUNNING) {
            throw new ConflictException("Wait for the run to finish before analyzing it");
        }
        List<TestResult> todo = resultRepository.findUnanalyzed(
                runId, ResultStatus.FAILED, PageRequest.of(0, maxBatch));
        if (todo.isEmpty()) {
            return new AnalysisBatchResponse(0, maxBatch, "Nothing to analyze: no un-analyzed failures");
        }
        if (!worker.reserve(runId)) {
            throw new ConflictException("An analysis batch is already running for this run");
        }
        try {
            worker.analyzeFailures(projectId, runId, maxBatch);
        } catch (TaskRejectedException e) {
            worker.release(runId);
            throw new ConflictException("The AI queue is busy. Try again shortly");
        }
        return new AnalysisBatchResponse(todo.size(), maxBatch,
                "Analysis started. Poll GET .../analyses to see results as they arrive");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AiAnalysisResponse> list(Long projectId, Long runId, User owner, Pageable pageable) {
        findOwnedExecution(projectId, runId, owner);
        return PageResponse.from(analysisRepository.findByExecutionId(runId, pageable)
                .map(AiAnalysisResponse::from));
    }

    // ---------- helpers: ownership chain project -> run -> result ----------

    private TestExecution findOwnedExecution(Long projectId, Long runId, User owner) {
        projectRepository.findByIdAndOwnerId(projectId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        return executionRepository.findByIdAndProjectId(runId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Run not found with id: " + runId));
    }

    private TestResult findOwnedResult(Long projectId, Long runId, Long resultId, User owner) {
        findOwnedExecution(projectId, runId, owner);
        return resultRepository.findByIdAndExecutionId(resultId, runId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + resultId));
    }
}