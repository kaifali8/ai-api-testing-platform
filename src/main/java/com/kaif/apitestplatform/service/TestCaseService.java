package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.request.UpdateTestCaseRequest;
import com.kaif.apitestplatform.dto.response.TestCaseResponse;
import com.kaif.apitestplatform.entity.User;

public interface TestCaseService {
    TestCaseResponse update(Long projectId, Long suiteId, Long testId,
                            UpdateTestCaseRequest request, User owner);

    void delete(Long projectId, Long suiteId, Long testId, User owner);
}
