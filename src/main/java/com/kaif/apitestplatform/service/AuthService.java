package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.request.LoginRequest;
import com.kaif.apitestplatform.dto.request.RegisterRequest;
import com.kaif.apitestplatform.dto.response.AuthResponse;

import java.nio.file.AccessDeniedException;

public interface AuthService {
    AuthResponse register(RegisterRequest request) throws AccessDeniedException;
    AuthResponse login(LoginRequest request);
}
