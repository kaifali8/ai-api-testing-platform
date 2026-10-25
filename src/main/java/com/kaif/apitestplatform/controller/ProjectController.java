package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.request.ProjectRequest;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.dto.response.ProjectResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request,user));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> list (
            @PageableDefault(size = 10, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal User user){
        return ResponseEntity.ok(projectService.list(user,pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(
        @PathVariable Long id, @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(projectService.get(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(projectService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,@AuthenticationPrincipal User user
    ){
        projectService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
