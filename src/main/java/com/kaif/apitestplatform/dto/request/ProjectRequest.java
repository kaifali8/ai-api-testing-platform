package com.kaif.apitestplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100,message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 500,message = "Description must be at most 500 characters")
    private String description;

    @NotBlank(message = "Base URL is required")
    @URL(message = "Base URL must be a valid URL")
    private String baseUrl;
}
