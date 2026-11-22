package com.kaif.apitestplatform.ai;

import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.exception.AiServiceException;
import com.kaif.apitestplatform.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisWorker {

    private final AiAnalysisCore core;
    private final TestResultRepository resultRepository;

    // One batch per run at a time (single server instance assumed)
    private final Set<Long> active = ConcurrentHashMap.newKeySet();

    @Value("${app.ai.delay-ms:1500}")
    private long delayMs;

    public boolean reserve(Long executionId) {
        return active.add(executionId);
    }

    public void release(Long executionId) {
        active.remove(executionId);
    }

    public boolean isActive(Long executionId) {
        return active.contains(executionId);
    }

    @Async("aiExecutor")
    public void analyzeFailures(Long projectId, Long executionId, int max) {
        try {
            List<TestResult> todo = resultRepository.findUnanalyzed(
                    executionId, ResultStatus.FAILED, PageRequest.of(0, max));
            for (TestResult r : todo) {
                try {
                    core.analyzeAndStore(r, projectId);
                } catch (AiServiceException e) {
                    log.warn("AI analysis of result {} failed: {}", r.getId(), e.getMessage());
                    // Rate-limited or service down: stop instead of hammering it
                    if (e.getStatus() == HttpStatus.TOO_MANY_REQUESTS
                            || e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
                        break;
                    }
                }
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("AI batch for run {} aborted", executionId, e);
        } finally {
            release(executionId);
        }
    }
}