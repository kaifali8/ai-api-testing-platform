package com.kaif.apitestplatform.dto.response;

import com.kaif.apitestplatform.enums.TestCategory;

import java.util.Map;

public record GenerationSummaryResponse (
        Long suiteId, String suitName,
        int endpointsProceed, int totalTests,
        Map<TestCategory, Long> testsByCategory
) { }
