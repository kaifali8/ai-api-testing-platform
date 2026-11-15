package com.kaif.apitestplatform.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.TestCase;
import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.exception.UnsafeUrlException;
import com.kaif.apitestplatform.repository.*;
import com.kaif.apitestplatform.security.SecretCipher;
import com.kaif.apitestplatform.testgen.RequestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunExecutor {

    private static final int MAX_STORED_CHARS = 20_000;

    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final TestCaseRepository testCaseRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final ProjectCredentialRepository credentialRepository;
    private final SecretCipher cipher;
    private final TestRequestBuilder requestBuilder;
    private final HttpTestClient httpClient;
    private final ResponseValidator validator;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.execution.delay-ms:100}")
    private long delayMs;

    /** Runs on a background thread. Never throws: every outcome is written to the database. */
    @Async("testRunExecutor")
    public void execute(Long executionId) {
        TestExecution exec = executionRepository.findById(executionId).orElse(null);
        if (exec == null) return;

        try {
            exec.setStatus(RunStatus.RUNNING);
            exec.setStartedAt(LocalDateTime.now());
            executionRepository.save(exec);

            runAll(exec);
            exec.setStatus(RunStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Test run {} aborted", executionId, e);
            exec.setStatus(RunStatus.FAILED);
            exec.setNote("Run aborted because of an internal error");
        } finally {
            exec.setFinishedAt(LocalDateTime.now());
            executionRepository.save(exec);
        }
    }

    private void runAll(TestExecution exec) throws InterruptedException {
        ResolvedCredential credential = credentialRepository.findByProjectId(exec.getProjectId())
                .map(c -> new ResolvedCredential(c.getAuthType(), c.getHeaderName(),
                        c.getUsername(), cipher.decrypt(c.getEncryptedSecret())))
                .orElse(null);

        List<String> secrets = credential == null
                ? List.of() : List.of(credential.secret(), credential.value());

        Map<String, Optional<String>> schemaCache = new HashMap<>();

        int pageNo = 0;
        Page<TestCase> page;
        do {
            page = testCaseRepository.findBySuiteIdAndEnabledTrue(
                    exec.getSuiteId(), PageRequest.of(pageNo++, 50, Sort.by("id")));

            for (TestCase test : page.getContent()) {
                String schemas = schemasFor(schemaCache, exec.getProjectId(), test);
                TestResult result = runOne(exec, test, credential, secrets, schemas);

                switch (result.getStatus()) {
                    case PASSED -> exec.setPassed(exec.getPassed() + 1);
                    case FAILED -> exec.setFailed(exec.getFailed() + 1);
                    case ERROR -> exec.setErrored(exec.getErrored() + 1);
                }
                resultRepository.save(result);
                executionRepository.save(exec);          // progress is visible while running

                if (delayMs > 0) Thread.sleep(delayMs);   // simple politeness towards the target API
            }
        } while (page.hasNext());
    }

    private TestResult runOne(TestExecution exec, TestCase test, ResolvedCredential credential,
                              List<String> secrets, String responseSchemas) {
        TestResult r = TestResult.builder()
                .executionId(exec.getId())
                .testCaseId(test.getId())
                .testName(test.getName())
                .category(test.getCategory())
                .httpMethod(test.getHttpMethod())
                .path(test.getPath())
                .expectedStatuses(test.getExpectedStatuses())
                .executedAt(LocalDateTime.now())
                .build();
        try {
            RequestData data = mapper.readValue(test.getRequestData(), RequestData.class);
            BuiltRequest request = requestBuilder.build(exec.getBaseUrl(), test.getHttpMethod(),
                    test.getPath(), data, test.isIncludeAuth(), credential);

            // Store the request first, so even a failed send shows what we tried
            r.setRequestUrl(SecretMasker.mask(request.uri().toString(), secrets));
            r.setRequestHeaders(toJson(request.maskedHeaders()));
            r.setRequestBody(truncate(SecretMasker.mask(request.body(), secrets)));

            HttpResponseData response = httpClient.send(request);

            r.setActualStatus(response.status());
            r.setResponseTimeMs(response.timeMs());
            r.setResponseHeaders(toJson(maskHeaders(response.headers(), secrets)));
            r.setResponseBody(truncate(SecretMasker.mask(response.body(), secrets)));
            r.setResponseTruncated(response.truncated());

            List<String> failures = validator.validate(
                    ResponseValidator.parseStatuses(test.getExpectedStatuses()), response, responseSchemas);
            r.setStatus(failures.isEmpty() ? ResultStatus.PASSED : ResultStatus.FAILED);
            if (!failures.isEmpty()) r.setFailureReasons(String.join("\n", failures));

        } catch (IllegalArgumentException | UnsafeUrlException e) {
            r.setStatus(ResultStatus.ERROR);               // messages we wrote ourselves: safe to show
            r.setErrorMessage(cut(e.getMessage(), 500));
        } catch (IOException e) {
            r.setStatus(ResultStatus.ERROR);
            r.setErrorMessage(cut(SecretMasker.mask(
                    e.getClass().getSimpleName() + ": " + e.getMessage(), secrets), 500));
        } catch (Exception e) {
            log.warn("Unexpected error in test {}", test.getId(), e);
            r.setStatus(ResultStatus.ERROR);
            r.setErrorMessage("Unexpected error while executing this test");
        }
        return r;
    }

    // ---------- helpers ----------

    // Look schemas up by method + path (stable), not by endpoint ID (changes on re-import)
    private String schemasFor(Map<String, Optional<String>> cache, Long projectId, TestCase t) {
        return cache.computeIfAbsent(t.getHttpMethod() + " " + t.getPath(), k ->
                apiDefinitionRepository
                        .findByProjectIdAndPathAndHttpMethod(projectId, t.getPath(), t.getHttpMethod())
                        .map(ApiDefinition::getResponseSchemas)
        ).orElse(null);
    }

    private Map<String, String> maskHeaders(Map<String, String> headers, List<String> secrets) {
        Map<String, String> out = new LinkedHashMap<>();
        headers.forEach((k, v) -> out.put(k,
                SecretMasker.isSensitiveHeader(k) ? "***" : SecretMasker.mask(v, secrets)));
        return out;
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String truncate(String s) {
        return s == null || s.length() <= MAX_STORED_CHARS
                ? s : s.substring(0, MAX_STORED_CHARS) + "... [truncated]";
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}