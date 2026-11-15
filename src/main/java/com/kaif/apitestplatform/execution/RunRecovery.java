package com.kaif.apitestplatform.execution;

import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.enums.RunStatus;
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

    private final TestExecutionRepository executionRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedRuns() {
        List<TestExecution> stuck = executionRepository.findByStatusIn(
                List.of(RunStatus.QUEUED, RunStatus.RUNNING));
        if (stuck.isEmpty()) return;

        stuck.forEach(e -> {
            e.setStatus(RunStatus.FAILED);
            e.setNote("Interrupted by a server restart");
            e.setFinishedAt(LocalDateTime.now());
        });
        executionRepository.saveAll(stuck);
        log.warn("Marked {} interrupted test run(s) as FAILED", stuck.size());
    }
}