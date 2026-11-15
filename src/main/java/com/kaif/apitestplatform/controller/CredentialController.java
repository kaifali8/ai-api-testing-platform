package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.request.CredentialRequest;
import com.kaif.apitestplatform.dto.response.CredentialsResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.service.CredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/credentials")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService service;

    @PutMapping
    public ResponseEntity<CredentialsResponse> save(
            @PathVariable Long projectId,
            @Valid @RequestBody CredentialRequest request,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.ok(service.save(projectId, request, user));
    }

    @GetMapping
    public ResponseEntity<CredentialsResponse> get(
            @PathVariable Long projectId, @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(service.get(projectId, user));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId, @AuthenticationPrincipal User user
    ){
        service.delete(projectId, user);
        return ResponseEntity.noContent().build();
    }
}
