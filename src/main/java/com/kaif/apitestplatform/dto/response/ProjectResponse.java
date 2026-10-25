package com.kaif.apitestplatform.dto.response;

import com.kaif.apitestplatform.entity.Project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String baseUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static ProjectResponse from(Project p){
     return new ProjectResponse(
             p.getId(),p.getName(),p.getDescription()
             ,p.getBaseUrl(),p.getCreatedAt(),p.getUpdatedAt());
    }
}
