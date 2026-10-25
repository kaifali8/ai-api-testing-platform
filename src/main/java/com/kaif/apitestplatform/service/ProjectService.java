package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.request.ProjectRequest;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.ProjectResponse;
import com.kaif.apitestplatform.entity.User;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    ProjectResponse create(ProjectRequest request, User owner);
    PageResponse<ProjectResponse> list(User owner, Pageable pageable);
    ProjectResponse get(Long id,User owner);
    ProjectResponse update(Long id, ProjectRequest request,User owner);
    void delete(Long id,User owner);
}
