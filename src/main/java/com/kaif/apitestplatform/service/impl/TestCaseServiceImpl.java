package com.kaif.apitestplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.dto.request.UpdateTestCaseRequest;
import com.kaif.apitestplatform.dto.response.TestCaseResponse;
import com.kaif.apitestplatform.entity.TestCase;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.exception.BadRequestException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.ProjectRepository;
import com.kaif.apitestplatform.repository.TestCaseRepository;
import com.kaif.apitestplatform.repository.TestSuiteRepository;
import com.kaif.apitestplatform.service.TestCaseService;
import com.kaif.apitestplatform.testgen.RequestData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class TestCaseServiceImpl implements TestCaseService {

    private static final int MAX_REQUEST_DATA_CHARS = 50_000;

    private final ProjectRepository projectRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public TestCaseResponse update(Long projectId, Long suiteId, Long testId, UpdateTestCaseRequest request, User owner) {
        TestCase t = findOwnedTest(projectId, suiteId, testId, owner);

        if (request.getName()!=null){
            if (request.getName().isBlank()) throw new BadRequestException("Name must not be blank");
            t.setName(request.getName().trim());
        }
        if (request.getDescription()!=null) t.setDescription(request.getDescription());
        if (request.getRequestData()!=null) t.setDescription(serialize(request.getRequestData()));
        if (request.getExpectedStatuses()!=null) t.setExpectedStatuses(request.getExpectedStatuses());
        if (request.getIncludeAuth()!=null) t.setIncludeAuth(request.getIncludeAuth());
        if (request.getEnabled()!=null) t.setEnabled(request.getEnabled());

        return TestCaseResponse.from(testCaseRepository.saveAndFlush(t));
    }

    @Override
    public void delete(Long projectId, Long suiteId, Long testId, User owner) {
        testCaseRepository.delete(findOwnedTest(projectId, suiteId, testId, owner));
    }

    // ----------- helpers ----------------

    private TestCase findOwnedTest(Long projectId, Long suiteId, Long testId, User owner){
        projectRepository.findByIdAndOwnerId(projectId,owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: "+projectId));

        testSuiteRepository.findByIdAndProjectId(suiteId,projectId)
                .orElseThrow(()-> new ResourceNotFoundException("Test suite not found with id: "+suiteId));
        return testCaseRepository.findByIdAndSuiteId(testId,suiteId)
                .orElseThrow(()-> new ResourceNotFoundException("Test case not found with id: "+testId));
    }

    private String serialize(RequestData rd){
        if (rd.getPathParams()==null) rd.setPathParams(new LinkedHashMap<>());
        if (rd.getQueryParams()== null) rd.setQueryParams(new LinkedHashMap<>());
        if (rd.getHeaders()==null) rd.setHeaders(new LinkedHashMap<>());

        try {
            String json = mapper.writeValueAsString(rd);
            if (json.length() > MAX_REQUEST_DATA_CHARS){
                throw new BadRequestException("Request data is too large (max 50,000 characters");
            }
        }catch (JsonProcessingException e){
            throw new BadRequestException("Request data could not be processed");
        }
    }
}
