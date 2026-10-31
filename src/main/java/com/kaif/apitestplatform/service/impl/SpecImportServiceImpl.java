package com.kaif.apitestplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kaif.apitestplatform.dto.response.ApiDefinitionResponse;
import com.kaif.apitestplatform.dto.response.ImportSummaryResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.ApiParameter;
import com.kaif.apitestplatform.entity.Project;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.exception.InvalidSpecException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.ApiDefinitionRepository;
import com.kaif.apitestplatform.repository.ProjectRepository;
import com.kaif.apitestplatform.service.SpecImportService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecImportServiceImpl implements SpecImportService {
    private final ProjectRepository projectRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;

    @Override
    @Transactional
    public ImportSummaryResponse importSpec(Long projectId, MultipartFile file, User user) {
        Project project=findOwnedProject(projectId,user);
        String content=readFile(file);

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult result=new OpenAPIV3Parser().readContents(content,null,options);

        OpenAPI openAPI=result.getOpenAPI();
        if (openAPI==null || openAPI.getPaths()==null|| openAPI.getPaths().isEmpty()){
            String reason=result.getMessages()==null || result.getMessages().isEmpty()
                    ?"no paths found"
                    :String.join(";",result.getMessages());
            throw new InvalidSpecException("Invalid OpenAPI specifications:"+reason);
        }
        boolean globalAuth=openAPI.getSecurity()!=null && !openAPI.getSecurity().isEmpty();

        List<ApiDefinition> definitions=new ArrayList<>();
        openAPI.getPaths().forEach((path,item)->
                item.readOperationsMap().forEach((method, operation) ->
                        definitions.add(toDefinition(
                                project,path,method,operation,item,globalAuth
                        ))));

        apiDefinitionRepository.deleteByProjectId(projectId);
        apiDefinitionRepository.saveAll(definitions);

        int paramCount=definitions.stream().mapToInt(d-> d.getParameters().size()).sum();
        String title=openAPI.getInfo()!=null ? openAPI.getInfo().getTitle():null;
        String version= openAPI.getInfo() !=null ? openAPI.getInfo().getVersion():null;
        List<String> warnings = result.getMessages()==null?List.of():result.getMessages();

        return new ImportSummaryResponse(title,version,definitions.size(),paramCount,warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApiDefinitionResponse> listEndpoints(Long projectId, User owner, Pageable pageable) {
        findOwnedProject(projectId,owner);
        return PageResponse.from(apiDefinitionRepository
                .findByProjectId(projectId,pageable)
                .map(ApiDefinitionResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiDefinitionResponse getEndpoint(Long projectId, Long endpointId, User owner) {
        findOwnedProject(projectId,owner);
        return apiDefinitionRepository.findByIdAndProjectId(endpointId,projectId)
                .map(ApiDefinitionResponse::from)
                .orElseThrow(()-> new ResourceNotFoundException("Endpoint not found with id:"+endpointId));
    }


    //-------------Helpers-----------------
    private Project findOwnedProject(Long projectId, User user) {
        return projectRepository.findByIdAndOwnerId(projectId, user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Project not found with id:"+projectId));
    }

    private String readFile(MultipartFile file){
        if (file==null || file.isEmpty()){
            throw new InvalidSpecException("Upload file is empty");
        }
        String name= Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        if (!(name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml"))){
            throw new InvalidSpecException("Only .json, .yaml or .yml files are allowed");
        }
        try{
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }catch (IOException e){
            throw new InvalidSpecException("Could not read uplaoded file");
        }
    }

    private ApiDefinition toDefinition(Project project, String path,
                                       PathItem.HttpMethod method, Operation op,
                                       PathItem pathItem,boolean globalAuth){
        boolean requiresAuth=op.getSecurity() != null
                ? !op.getSecurity().isEmpty()
                : globalAuth;

        ApiDefinition def = ApiDefinition.builder()
                .project(project)
                .path(path)
                .httpMethod(method.name())
                .operationId(op.getOperationId())
                .summary(op.getSummary())
                .description(op.getDescription())
                .requiresAuth(requiresAuth)
                .requestBodySchema(extractRequestBodySchema(op))
                .responseSchemas(extractResponseBodySchema(op))
                .build();

        Map<String, Parameter> merged = new LinkedHashMap<>();
        if (pathItem.getParameters()!=null){
            pathItem.getParameters().forEach(p -> merged.put(p.getIn()+":"+ p.getName(),p));
        }
        if (op.getParameters()!=null){
            op.getParameters().forEach(p->merged.put(p.getIn()+":"+p.getName(),p));
        }
        merged.values().forEach(p-> def.getParameters().add(toParameter(def,p)));
        return def;
    }

    private ApiParameter toParameter(ApiDefinition def,Parameter p){
        Schema<?> s = p.getSchema();
        ApiParameter.ApiParameterBuilder b=ApiParameter.builder()
                .apiDefinition(def)
                .name(p.getName())
                .location(p.getIn())
                .required(Boolean.TRUE.equals(p.getRequired()))
                .description(p.getDescription());

        if (s!=null){
            b.type(schemaType(s))
                    .format(s.getFormat())
                    .minimum(s.getMinimum())
                    .maximum(s.getMaximum())
                    .minLength(s.getMinLength())
                    .maxLength(s.getMaxLength())
                    .pattern(s.getPattern());
            if (s.getEnum()!=null){
                b.enumValues(s.getEnum().stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
        }
        return b.build();
    }

    private String schemaType(Schema<?> s){
        if (s.getType() != null) return s.getType();
        if (s.getTypes() != null && !s.getTypes().isEmpty()) return s.getTypes().iterator().next();
        return null;
    }

    private String extractRequestBodySchema(Operation op){
        if (op.getRequestBody()==null) return null;
        Schema<?> schema=firstJsonSchema(op.getRequestBody().getContent());
        return toJson(schema);
    }

    private String extractResponseBodySchema(Operation op){
        if (op.getResponses() == null) return null;
        Map<String,Object> out=new LinkedHashMap<>();
        for (Map.Entry<String, ApiResponse> e: op.getResponses().entrySet()){
            Schema<?> schema= firstJsonSchema(e.getValue().getContent());
            out.put(e.getKey(),schema);
        }
        return toJson(out);
    }

    private Schema<?> firstJsonSchema(Content content){
        if (content==null || content.isEmpty()) return null;
        MediaType mt = content.get("application/json");
        if (mt==null) mt=content.values().iterator().next();
        return mt.getSchema();
    }

    private String toJson(Object o){
        if (o==null)return null;
        try {
            return Json.mapper().writeValueAsString(o);
        }catch (JsonProcessingException e){
            return null;
        }
    }
}
