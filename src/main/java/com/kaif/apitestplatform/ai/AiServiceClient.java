package com.kaif.apitestplatform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class AiServiceClient {

    private final RestClient client;
    private final String internalKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiServiceClient(@Value("${app.ai.base-url}") String baseUrl,
                           @Value("${app.ai.internal-key}") String internalKey,
                           @Value("${app.ai.timeout-seconds:75}") int timeoutSeconds) {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.internalKey = internalKey;
    }

    public AiPayloads.Parsed analyzeFailure(Map<String, Object> request) {
        return AiPayloads.parse(post("/analyze-failure", request));
    }

    /** Returns the raw JSON; LlmPayloads.parse() checks it. */
    public String generateTests(Map<String, Object> request) {
        return post("/generate-tests", request);
    }

    private String post(String uri, Map<String, Object> request) {
        String json;
        try {
            json = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Could not prepare the AI request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            return client.post()
                    .uri(uri)
                    .header("X-Internal-Key", internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 429) {
                throw new AiServiceException("AI rate limit reached. Try again shortly", HttpStatus.TOO_MANY_REQUESTS);
            }
            log.warn("AI service answered HTTP {} for {}", code, uri);   // 401 means the shared keys differ
            throw new AiServiceException("AI service could not process this request", HttpStatus.BAD_GATEWAY);
        } catch (RestClientException e) {
            log.warn("AI service not reachable: {}", e.getClass().getSimpleName());
            throw new AiServiceException("AI service is not reachable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}