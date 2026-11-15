package com.kaif.apitestplatform.dto.response;

import com.kaif.apitestplatform.entity.ProjectCredentials;
import com.kaif.apitestplatform.enums.AuthType;

import java.time.LocalDateTime;

public record CredentialsResponse(
        boolean configured, AuthType authType,
        String headerName, String username, LocalDateTime updatedAt
) {
    public static CredentialsResponse none(){
        return new CredentialsResponse(false,null,null,null,null);
    }

    public static CredentialsResponse from(ProjectCredentials c){
        return new CredentialsResponse(true,c.getAuthType(),
                c.getHeaderName(),c.getUsername(),c.getUpdatedAt());
    }
}
