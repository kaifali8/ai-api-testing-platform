package com.kaif.apitestplatform.execution;

import com.kaif.apitestplatform.entity.LlmGenerationJob;
import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.repository.LlmGenerationJobRepository;
import com.kaif.apitestplatform.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunRecovery {

    private static final List<RunStatus> ACTIVE = List.of(RunStatus.QUEUED, RunStatus.RUNNING);

    private final TestExecutionRepository executionRepository;
    private final LlmGenerationJobRepository jobRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedWork() {
        List<TestExecution> runs = executionRepository.findByStatusIn(ACTIVE);
        runs.forEach(e -> {
            e.setStatus(RunStatus.FAILED);
            e.setNote("Interrupted by a server restart");
            e.setFinishedAt(LocalDateTime.now());
        });
        executionRepository.saveAll(runs);

        List<LlmGenerationJob> jobs = jobRepository.findByStatusIn(ACTIVE);
        jobs.forEach(j -> {
            j.setStatus(RunStatus.FAILED);
            j.setNote("Interrupted by a server restart");
            j.setFinishedAt(LocalDateTime.now());
        });
        jobRepository.saveAll(jobs);

        if (!runs.isEmpty() || !jobs.isEmpty()) {
            log.warn("Marked {} test run(s) and {} generation job(s) as FAILED after restart",
                    runs.size(), jobs.size());
        }
    }
}