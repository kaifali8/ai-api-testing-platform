package com.kaif.apitestplatform.testgen;

import com.kaif.apitestplatform.enums.TestCategory;

public record GeneratedCase (
        String name,
        String description,
        TestCategory category,
        String requestDataJson,
        String expectedStatuses,
        boolean includeAuth
){ }
