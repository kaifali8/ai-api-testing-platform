package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.dto.request.ProjectRequest;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.ProjectResponse;
import com.kaif.apitestplatform.entity.Project;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.exception.ConflictException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.*;
import com.kaif.apitestplatform.security.UrlGuard;
import com.kaif.apitestplatform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final UrlGuard urlGuard;
    private final ProjectCredentialRepository projectCredentialRepository;
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final AiAnalysisRepository aiAnalysisRepository;

    @Override
    public ProjectResponse create(ProjectRequest request, User owner) {
        urlGuard.validate(request.getBaseUrl());
        Project project=Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .baseUrl(request.getBaseUrl())
                .owner(owner)
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(User owner, Pageable pageable) {
        return PageResponse.from(
                projectRepository.findByOwnerId(owner.getId(), pageable)
                        .map(ProjectResponse::from));
    }

    @Override
    @Transactional
    public ProjectResponse get(Long id,User owner) {
        return ProjectResponse.from(findOwned(id,owner));
    }

    @Override
    public ProjectResponse update(Long id, ProjectRequest request, User owner) {
        Project project=findOwned(id, owner);
        urlGuard.validate(request.getBaseUrl());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setBaseUrl(request.getBaseUrl());
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    @Override
    public void delete(Long id, User owner) {
        Project project = findOwned(id, owner);
        if (executionRepository.existsByProjectIdAndStatusIn(id,
                List.of(RunStatus.QUEUED, RunStatus.RUNNING))) {
            throw new ConflictException("A test run is in progress. Wait for it to finish before deleting the project");
        }
        aiAnalysisRepository.deleteByProjectId(id);
        resultRepository.deleteByProjectId(id);     // results first: its sub-select needs the executions
        executionRepository.deleteByProjectId(id);
        projectCredentialRepository.deleteByProjectId(id);
        testSuiteRepository.deleteByProjectId(id);
        apiDefinitionRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    private Project findOwned(Long id,User owner){
        return projectRepository.findByIdAndOwnerId(id,owner.getId())
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Project not found with id:"+id
                ));
    }

}
