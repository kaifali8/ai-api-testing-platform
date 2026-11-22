package com.kaif.apitestplatform.ai;

import com.kaif.apitestplatform.entity.AiAnalysis;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.repository.AiAnalysisRepository;
import com.kaif.apitestplatform.repository.ApiDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiAnalysisCore {

    private final AiAnalysisRepository analysisRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final AiServiceClient client;

    /** Returns the cached analysis if one exists, otherwise calls the AI and stores the result. */
    public AiAnalysis analyzeAndStore(TestResult r, Long projectId) {
        Optional<AiAnalysis> existing = analysisRepository.findByResultId(r.getId());
        if (existing.isPresent()) return existing.get();

        String summary = apiDefinitionRepository
                .findByProjectIdAndPathAndHttpMethod(projectId, r.getPath(), r.getHttpMethod())
                .map(ApiDefinition::getSummary).orElse(null);

        AiPayloads.Parsed p = client.analyzeFailure(AiPayloads.buildRequest(r, summary));

        AiAnalysis a = AiAnalysis.builder()
                .resultId(r.getId())
                .executionId(r.getExecutionId())
                .likelyCause(p.likelyCause()).evidence(p.evidence())
                .severity(p.severity()).confidence(p.confidence())
                .expectedBehavior(p.expectedBehavior()).actualBehavior(p.actualBehavior())
                .remediation(p.remediation()).model(p.model())
                .build();
        try {
            return analysisRepository.save(a);
        } catch (DataIntegrityViolationException e) {
            // Two requests analysed the same result at once; the unique constraint kept one
            return analysisRepository.findByResultId(r.getId()).orElseThrow(() -> e);
        }
    }
}