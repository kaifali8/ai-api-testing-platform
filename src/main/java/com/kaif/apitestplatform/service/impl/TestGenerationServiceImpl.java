package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.dto.response.GenerationSummaryResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.TestCaseResponse;
import com.kaif.apitestplatform.dto.response.TestSuiteResponse;
import com.kaif.apitestplatform.entity.*;
import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.enums.TestSource;
import com.kaif.apitestplatform.exception.BadRequestException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.ApiDefinitionRepository;
import com.kaif.apitestplatform.repository.ProjectRepository;
import com.kaif.apitestplatform.repository.TestCaseRepository;
import com.kaif.apitestplatform.repository.TestSuiteRepository;
import com.kaif.apitestplatform.service.TestGenerationService;
import com.kaif.apitestplatform.testgen.GeneratedCase;
import com.kaif.apitestplatform.testgen.RuleBasedTestGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestGenerationServiceImpl implements TestGenerationService {

    private final ProjectRepository projectRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final RuleBasedTestGenerator generator;

    @Override
    @Transactional
    public GenerationSummaryResponse generate(Long projectId, String suitName, User owner) {
        Project project = findOwnedProject(projectId, owner);

        List<ApiDefinition> apis = apiDefinitionRepository.findAllByProjectId(projectId);
        if (apis.isEmpty()){
            throw new BadRequestException(
                    "No endpoints found. Import an OpenAPI specifications first."
            );
        }

        TestSuite suite = testSuiteRepository.save(TestSuite.builder()
                        .project(project)
                        .name(resolveName(suitName))
                .build());

        List<TestCase> tests = new ArrayList<>();
        for (ApiDefinition api:apis){
            for (GeneratedCase gc: generator.generate(api)){
                tests.add(toEntity(suite,api,gc));
            }
        }
        testCaseRepository.saveAll(tests);

        Map<TestCategory,Long> byCategory = tests.stream().collect(Collectors.groupingBy(
                TestCase::getCategory, () -> new EnumMap<>(TestCategory.class),Collectors.counting()
        ));

        return new GenerationSummaryResponse(
                suite.getId(),suite.getName(),apis.size(),tests.size(),byCategory
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TestSuiteResponse> listSuites(Long projectId, User owner, Pageable pageable) {
        findOwnedProject(projectId,owner);
        return PageResponse.from(testSuiteRepository.findByProjectId(projectId,pageable)
                .map(s-> new TestSuiteResponse(
                        s.getId(),s.getName(),s.getCreatedAt(),
                        testCaseRepository.countBySuiteId(s.getId())
                )));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TestCaseResponse> listTests(Long projectId, Long suiteId, TestCategory category, User owner, Pageable pageable) {
        findOwnedProject(projectId, owner);
        testSuiteRepository.findByIdAndProjectId(suiteId,projectId)
                .orElseThrow(()-> new ResourceNotFoundException("Test suite not found with id: "+suiteId));
        Page<TestCase> page = (category == null)
                ? testCaseRepository.findBySuiteId(suiteId, pageable)
                : testCaseRepository.findBySuiteIdAndCategory(suiteId, category, pageable);
        return PageResponse.from(page.map(TestCaseResponse::from));
    }

    // ------ helpers -------

    private Project findOwnedProject(Long projectId, User owner){
        return projectRepository.findByIdAndOwnerId(projectId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: "+ projectId));
    }

    private String resolveName(String requested){
        if (requested == null || requested.isBlank()){
            return "Suite " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        String name = requested.trim();
        if (name.length()>100){
            throw new BadRequestException("Suite name must be at most 100 characters");
        }
        return name;
    }

    private TestCase toEntity(TestSuite suite, ApiDefinition api, GeneratedCase gc){
        return TestCase.builder()
                .suite(suite)
                .endpointId(api.getId())
                .httpMethod(api.getHttpMethod())
                .path(api.getPath())
                .name(cut(gc.name(),300))
                .description(cut(gc.description(),500))
                .category(gc.category())
                .source(TestSource.RULE)
                .requestData(gc.requestDataJson())
                .expectedStatuses(gc.expectedStatuses())
                .includeAuth(gc.includeAuth())
                .build();
    }

    private String cut(String s,int max){
        return s == null || s.length() <= max ? s: s.substring(0,max);
    }
}
