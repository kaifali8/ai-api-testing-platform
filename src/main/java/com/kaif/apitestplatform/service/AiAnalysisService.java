package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.response.AiAnalysisResponse;
import com.kaif.apitestplatform.dto.response.AnalysisBatchResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.User;
import org.springframework.data.domain.Pageable;

public interface AiAnalysisService {
    AiAnalysisResponse analyze(Long projectId, Long runId, Long resultId, User owner);
    AiAnalysisResponse get(Long projectId, Long runId, Long resultId, User owner);
    AnalysisBatchResponse analyzeRun(Long projectId, Long runId, User owner);
    PageResponse<AiAnalysisResponse> list(Long projectId, Long runId, User owner, Pageable pageable);
}