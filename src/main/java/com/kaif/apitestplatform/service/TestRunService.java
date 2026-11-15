package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.response.ExecutionResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.TestResultResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.ResultStatus;
import org.springframework.data.domain.Pageable;

public interface TestRunService {
    ExecutionResponse start(Long projectId, Long suiteId, User owner);
    PageResponse<ExecutionResponse> list(Long projectId, User owner, Pageable pageable);
    ExecutionResponse get(Long projectId, Long executionId, User owner);
    PageResponse<TestResultResponse> results(Long projectId, Long executionId,
                                             ResultStatus status, User owner, Pageable pageable);
}