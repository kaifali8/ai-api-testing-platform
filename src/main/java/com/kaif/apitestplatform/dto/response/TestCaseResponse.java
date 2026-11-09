package com.kaif.apitestplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.kaif.apitestplatform.entity.TestCase;
import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.enums.TestSource;

public record TestCaseResponse(
        Long id, String httpMethod, String path,
        String name, String description,
        TestCategory category, TestSource source,
        @JsonRawValue String requestData,
        String expectedStatuses,boolean includeAuth, boolean enabled
) {
    public static TestCaseResponse from(TestCase t){
        return new TestCaseResponse(
                t.getId(),t.getHttpMethod(),t.getPath(),
                t.getName(),t.getDescription(),
                t.getCategory(),t.getSource(),
                t.getRequestData(),t.getExpectedStatuses(),
                t.isIncludeAuth(),t.isEnabled()
        );
    }
}
