package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.request.CredentialRequest;
import com.kaif.apitestplatform.dto.response.CredentialsResponse;
import com.kaif.apitestplatform.entity.User;

public interface CredentialService {
    CredentialsResponse save(Long projectId, CredentialRequest request, User owner);
    CredentialsResponse get(Long projectId, User owner);
    void delete(Long projectId, User owner);
}
