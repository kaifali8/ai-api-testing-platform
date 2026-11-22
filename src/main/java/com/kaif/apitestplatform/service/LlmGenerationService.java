package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.response.LlmGenerationJobResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.User;
import org.springframework.data.domain.Pageable;

public interface LlmGenerationService {
    LlmGenerationJobResponse start(Long projectId, Long suiteId, int maxPerEndpoint, User owner);
    LlmGenerationJobResponse get(Long projectId, Long jobId, User owner);
    PageResponse<LlmGenerationJobResponse> list(Long projectId, User owner, Pageable pageable);
}