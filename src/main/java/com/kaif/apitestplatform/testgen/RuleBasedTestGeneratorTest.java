package com.kaif.apitestplatform.testgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.ApiParameter;
import com.kaif.apitestplatform.enums.TestCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTestGeneratorTest {

    private final RuleBasedTestGenerator generator = new RuleBasedTestGenerator();

    private ApiDefinition endPoint(boolean auth, ApiParameter...params){
        ApiDefinition api = ApiDefinition.builder()
                .path("/items").httpMethod("GET").requiresAuth(auth)
                .responseSchemas("{\"200\":null}")
                .build();
        api.getParameters().addAll(List.of(params));
        return api;
    }

    private ApiParameter limitParam(){
        return ApiParameter.builder()
                .name("limit").location("query").required(true).type("integer")
                .minimum(BigDecimal.ONE).maximum(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    void alwaysGeneratesExactlyOnePositiveTest(){
        List<GeneratedCase> cases = generator.generate(endPoint(false,limitParam()));
        assertThat(cases).filteredOn(c-> c.category() == TestCategory.BOUNDARY).hasSize(3);
    }

    @Test
    void missingRequiredQueryParamExpectsClientError(){
        List<GeneratedCase> cases = generator.generate(endPoint(false,limitParam()));
        assertThat(cases).anySatisfy(c-> {
            assertThat(c.name()).isEqualTo("Missing required query parameter 'limit'");
            assertThat(c.expectedStatuses()).isEqualTo("400,422");
        });
    }

    @Test
    void numericRangeProducesBoundaryTestsWithoutDuplicates(){
        List<GeneratedCase> cases = generator.generate(endPoint(false,limitParam()));

        assertThat(cases).filteredOn(c -> c.category() == TestCategory.BOUNDARY).hasSize(3);
        assertThat(cases).hasSize(6);
    }

    @Test
    void authenticatedEndpointGetsAnonymousTest(){
        List<GeneratedCase> cases = generator.generate(endPoint(true,limitParam()));
        assertThat(cases).filteredOn(c -> c.category() == TestCategory.AUTHENTICATION)
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.includeAuth()).isFalse();
                    assertThat(c.expectedStatuses()).isEqualTo("401,403");
                });
    }
}
