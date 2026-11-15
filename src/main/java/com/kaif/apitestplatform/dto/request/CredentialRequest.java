package com.kaif.apitestplatform.dto.request;

import com.kaif.apitestplatform.enums.AuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class CredentialRequest {

    @NotNull(message = "authType is required")
    private AuthType authType;

    @Size(max=100,message = "headerName must be at most 100 characters")
    private String headerName;

    @Size(max = 200, message = "username must be at most 200 characters")
    private String username;

    @NotBlank(message = "secret is required")
    @Size(max = 2000,message = "secret must be at most 2000 characters")
    @ToString.Exclude //to not print secret in logs
    private String secret;
}
