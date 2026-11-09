package com.kaif.apitestplatform.dto.response;

import java.time.LocalDateTime;

public record TestSuiteResponse(
        Long id, String name, LocalDateTime createdAt, long testCount
) {
}
