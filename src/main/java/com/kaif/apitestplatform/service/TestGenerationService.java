package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.response.GenerationSummaryResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.TestCaseResponse;
import com.kaif.apitestplatform.dto.response.TestSuiteResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.TestCategory;
import org.springframework.data.domain.Pageable;

public interface TestGenerationService {
    GenerationSummaryResponse generate(Long projectId, String suitName, User owner);
    PageResponse<TestSuiteResponse> listSuites(Long projectId, User owner, Pageable pageable);
    PageResponse<TestCaseResponse> listTests(Long projectId, Long suiteId, TestCategory category,
                                             User owner,Pageable pageable);
}
