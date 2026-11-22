package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.ai.LlmGenerationWorker;
import com.kaif.apitestplatform.dto.response.LlmGenerationJobResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.LlmGenerationJob;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.exception.BadRequestException;
import com.kaif.apitestplatform.exception.ConflictException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.*;
import com.kaif.apitestplatform.service.LlmGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmGenerationServiceImpl implements LlmGenerationService {

    private static final List<RunStatus> ACTIVE = List.of(RunStatus.QUEUED, RunStatus.RUNNING);

    private final ProjectRepository projectRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final TestExecutionRepository executionRepository;
    private final LlmGenerationJobRepository jobRepository;
    private final LlmGenerationWorker worker;

    @Value("${app.ai.max-generation-endpoints:50}")
    private int maxEndpoints;

    @Override
    public LlmGenerationJobResponse start(Long projectId, Long suiteId, int maxPerEndpoint, User owner) {
        if (maxPerEndpoint < 1 || maxPerEndpoint > 10) {
            throw new BadRequestException("maxPerEndpoint must be between 1 and 10");
        }
        findOwnedProject(projectId, owner);
        testSuiteRepository.findByIdAndProjectId(suiteId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Test suite not found with id: " + suiteId));

        long endpoints = apiDefinitionRepository.countByProjectId(projectId);
        if (endpoints == 0) {
            throw new BadRequestException("No endpoints found. Import an OpenAPI specification first.");
        }
        // New tests must not appear in the middle of an execution
        if (executionRepository.existsByProjectIdAndStatusIn(projectId, ACTIVE)) {
            throw new ConflictException("A test run is in progress. Wait for it to finish");
        }
        if (jobRepository.existsByProjectIdAndStatusIn(projectId, ACTIVE)) {
            throw new ConflictException("An AI generation job is already running for this project");
        }

        LlmGenerationJob job = jobRepository.save(LlmGenerationJob.builder()
                .projectId(projectId)
                .suiteId(suiteId)
                .status(RunStatus.QUEUED)
                .maxPerEndpoint(maxPerEndpoint)
                .endpointsTotal((int) Math.min(endpoints, maxEndpoints))
                .build());

        try {
            worker.run(job.getId(), maxPerEndpoint);
        } catch (TaskRejectedException e) {
            job.setStatus(RunStatus.FAILED);
            job.setNote("Rejected: the AI queue is busy");
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            throw new ConflictException("The AI queue is busy. Try again shortly");
        }
        return LlmGenerationJobResponse.from(job);
    }

    @Override
    @Transactional(readOnly = true)
    public LlmGenerationJobResponse get(Long projectId, Long jobId, User owner) {
        findOwnedProject(projectId, owner);
        return jobRepository.findByIdAndProjectId(jobId, projectId)
                .map(LlmGenerationJobResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Generation job not found with id: " + jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LlmGenerationJobResponse> list(Long projectId, User owner, Pageable pageable) {
        findOwnedProject(projectId, owner);
        return PageResponse.from(jobRepository.findByProjectId(projectId, pageable)
                .map(LlmGenerationJobResponse::from));
    }

    private void findOwnedProject(Long projectId, User owner) {
        projectRepository.findByIdAndOwnerId(projectId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }
}