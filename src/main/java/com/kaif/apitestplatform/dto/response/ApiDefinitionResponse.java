package com.kaif.apitestplatform.dto.response;


import com.kaif.apitestplatform.entity.ApiDefinition;

import java.util.List;

public record ApiDefinitionResponse(
        Long id,String path, String httpMethod,
        String operationId, String summary, boolean requiresAuth,
        List<ApiParameterResponse> parameters, String requestBodySchema, String responseSchemas) {
    public static ApiDefinitionResponse from(ApiDefinition a){
        return new ApiDefinitionResponse(
                a.getId(), a.getPath(), a.getHttpMethod(),
                a.getOperationId(), a.getSummary(), a.isRequiresAuth(),
                a.getParameters().stream().map(ApiParameterResponse::from).toList(),
                a.getRequestBodySchema(), a.getResponseSchemas());
    }
}
