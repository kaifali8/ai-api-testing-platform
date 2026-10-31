package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.dto.request.ProjectRequest;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.ProjectResponse;
import com.kaif.apitestplatform.entity.Project;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.ApiDefinitionRepository;
import com.kaif.apitestplatform.repository.ProjectRepository;
import com.kaif.apitestplatform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;

    @Override
    public ProjectResponse create(ProjectRequest request, User owner) {
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
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setBaseUrl(request.getBaseUrl());
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    @Override
    public void delete(Long id, User owner) {
        Project project=findOwned(id, owner);
        apiDefinitionRepository.deleteByProjectId(id);//children first deleted
        projectRepository.delete(project);
    }

    private Project findOwned(Long id,User owner){
        return projectRepository.findByIdAndOwnerId(id,owner.getId())
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Project not found with id:"+id
                ));
    }

}
