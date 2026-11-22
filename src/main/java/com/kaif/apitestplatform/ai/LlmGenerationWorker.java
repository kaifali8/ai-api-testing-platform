package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.LlmGenerationJob;
import com.kaif.apitestplatform.entity.TestCase;
import com.kaif.apitestplatform.entity.TestSuite;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.enums.TestSource;
import com.kaif.apitestplatform.exception.AiServiceException;
import com.kaif.apitestplatform.repository.*;
import com.kaif.apitestplatform.testgen.GeneratedCase;
import com.kaif.apitestplatform.testgen.RequestData;
import com.kaif.apitestplatform.testgen.RuleBasedTestGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmGenerationWorker {

    private static final int PAGE_SIZE = 200;

    private final LlmGenerationJobRepository jobRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final RuleBasedTestGenerator ruleGenerator;
    private final AiServiceClient client;
    private final LlmTestValidator validator = new LlmTestValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.ai.generation-delay-ms:4000}")
    private long delayMs;

    @Value("${app.ai.max-generation-endpoints:50}")
    private int maxEndpoints;

    /** Background thread. Never throws: every outcome is written to the job row. */
    @Async("aiExecutor")
    public void run(Long jobId, int maxPerEndpoint) {
        LlmGenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;
        try {
            job.setStatus(RunStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);
            process(job, maxPerEndpoint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            job.setStatus(RunStatus.FAILED);
            job.setNote("Interrupted");
        } catch (Exception e) {
            log.error("LLM generation job {} aborted", jobId, e);
            job.setStatus(RunStatus.FAILED);
            job.setNote("Generation aborted because of an internal error");
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    private void process(LlmGenerationJob job, int maxPerEndpoint) throws InterruptedException {
        TestSuite suite = testSuiteRepository.findById(job.getSuiteId())
                .orElseThrow(() -> new IllegalStateException("Suite no longer exists"));

        List<ApiDefinition> apis = new ArrayList<>(apiDefinitionRepository.findAllByProjectId(job.getProjectId()));
        apis.sort(Comparator.comparing(ApiDefinition::getPath).thenComparing(ApiDefinition::getHttpMethod));
        String note = null;
        if (apis.size() > maxEndpoints) {
            apis = new ArrayList<>(apis.subList(0, maxEndpoints));
            note = "Limited to the first " + maxEndpoints + " endpoints";
        }
        job.setEndpointsTotal(apis.size());
        job.setNote(note);

        // What already exists: fingerprints (for dedup) and names (shown to the model)
        Set<String> seen = new HashSet<>();
        Map<String, List<String>> namesByEndpoint = new HashMap<>();
        loadExisting(job.getSuiteId(), seen, namesByEndpoint);

        Map<String, Integer> reasons = new TreeMap<>();
        for (ApiDefinition api : apis) {
            boolean stop = false;
            try {
                handleEndpoint(job, suite, api, maxPerEndpoint, seen, namesByEndpoint, reasons);
            } catch (AiServiceException e) {
                log.warn("Generation for {} {} failed: {}", api.getHttpMethod(), api.getPath(), e.getMessage());
                job.setEndpointsFailed(job.getEndpointsFailed() + 1);
                // 429 / 503 are systemic: every next call would fail too
                stop = e.getStatus() == HttpStatus.TOO_MANY_REQUESTS
                        || e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE;
            }
            job.setEndpointsProcessed(job.getEndpointsProcessed() + 1);
            job.setRejectionReasons(toJson(reasons));
            jobRepository.save(job);

            if (stop) {
                job.setStatus(RunStatus.FAILED);
                job.setNote("Stopped early: the AI provider was rate-limited or unavailable. "
                        + "Accepted tests were kept; run again to continue");
                return;
            }
            Thread.sleep(delayMs);
        }
        job.setStatus(RunStatus.COMPLETED);
    }

    private void handleEndpoint(LlmGenerationJob job, TestSuite suite, ApiDefinition api, int max,
                                Set<String> seen, Map<String, List<String>> namesByEndpoint,
                                Map<String, Integer> reasons) {
        String key = key(api.getHttpMethod(), api.getPath());
        List<String> names = namesByEndpoint.getOrDefault(key, List.of());

        String json = client.generateTests(LlmPayloads.buildRequest(api, baselineJson(api), names, max));
        LlmPayloads.Proposals proposals = LlmPayloads.parse(json);
        if (proposals.model() != null) job.setModel(proposals.model());

        List<TestCase> toSave = new ArrayList<>();
        for (JsonNode node : proposals.tests()) {
            job.setProposed(job.getProposed() + 1);

            LlmTestValidator.Outcome outcome = validator.validate(api, node);
            if (!outcome.isAccepted()) {
                reject(job, reasons, outcome.rejection());
                continue;
            }
            LlmTestValidator.Accepted a = outcome.accepted();

            String fingerprint = TestFingerprint.of(api.getHttpMethod(), api.getPath(), a.data(), a.includeAuth());
            if (seen.contains(fingerprint)) {
                job.setDuplicates(job.getDuplicates() + 1);
                continue;
            }
            if (toSave.size() >= max) {
                reject(job, reasons, "over_limit");
                continue;
            }
            seen.add(fingerprint);
            toSave.add(toEntity(suite, api, a));
        }

        if (!toSave.isEmpty()) {
            testCaseRepository.saveAll(toSave);
            job.setAccepted(job.getAccepted() + toSave.size());
            namesByEndpoint.computeIfAbsent(key, k -> new ArrayList<>())
                    .addAll(toSave.stream().map(TestCase::getName).toList());
        }
    }

    // ---------- helpers ----------

    private void loadExisting(Long suiteId, Set<String> seen, Map<String, List<String>> names) {
        int pageNo = 0;
        Page<TestCase> page;
        do {
            page = testCaseRepository.findBySuiteId(suiteId, PageRequest.of(pageNo++, PAGE_SIZE, Sort.by("id")));
            for (TestCase t : page.getContent()) {
                RequestData rd = readData(t.getRequestData());
                if (rd != null) {
                    seen.add(TestFingerprint.of(t.getHttpMethod(), t.getPath(), rd, t.isIncludeAuth()));
                }
                names.computeIfAbsent(key(t.getHttpMethod(), t.getPath()), k -> new ArrayList<>()).add(t.getName());
            }
        } while (page.hasNext());
    }

    // The rule generator's valid request: a known-good example for the model to copy and tweak
    private String baselineJson(ApiDefinition api) {
        return ruleGenerator.generate(api).stream()
                .filter(c -> c.category() == TestCategory.POSITIVE)
                .map(GeneratedCase::requestDataJson)
                .findFirst().orElse(null);
    }

    private TestCase toEntity(TestSuite suite, ApiDefinition api, LlmTestValidator.Accepted a) {
        return TestCase.builder()
                .suite(suite)
                .endpointId(api.getId())
                .httpMethod(api.getHttpMethod())
                .path(api.getPath())
                .name(a.name())
                .description(a.description())
                .category(a.category())
                .source(TestSource.LLM)
                .requestData(a.requestDataJson())
                .expectedStatuses(a.expectedStatuses())
                .includeAuth(a.includeAuth())
                .build();
    }

    private void reject(LlmGenerationJob job, Map<String, Integer> reasons, String reason) {
        job.setInvalid(job.getInvalid() + 1);
        reasons.merge(reason, 1, Integer::sum);
    }

    private RequestData readData(String json) {
        try {
            return mapper.readValue(json, RequestData.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String key(String method, String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path;
    }
}