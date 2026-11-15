package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.dto.request.CredentialRequest;
import com.kaif.apitestplatform.dto.response.CredentialsResponse;
import com.kaif.apitestplatform.entity.Project;
import com.kaif.apitestplatform.entity.ProjectCredentials;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.AuthType;
import com.kaif.apitestplatform.exception.BadRequestException;
import com.kaif.apitestplatform.exception.ResourceNotFoundException;
import com.kaif.apitestplatform.repository.ProjectCredentialRepository;
import com.kaif.apitestplatform.repository.ProjectRepository;
import com.kaif.apitestplatform.security.SecretCipher;
import com.kaif.apitestplatform.service.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CredentialServiceImpl implements CredentialService {

    private static final Pattern HEADER_NAME = Pattern.compile("^[A-Za-z0-9-]{1,100}$");
    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "host","content-length","transfer-encoding","connection",
            "upgrade","expect","te","trailer"
    );

    private final ProjectRepository projectRepository;
    private final ProjectCredentialRepository projectCredentialRepository;
    private final SecretCipher secretCipher;

    @Override
    public CredentialsResponse save(Long projectId, CredentialRequest request, User owner) {
        Project project = findOwnedProject(projectId,owner);
        validate(request);

        ProjectCredentials credentials = projectCredentialRepository.findByProjectId(projectId)
                .orElseGet(()-> ProjectCredentials.builder().project(project).build());

        credentials.setAuthType(request.getAuthType());
        credentials.setHeaderName(request.getAuthType() == AuthType.API_KEY ? request.getHeaderName().trim() : null);
        credentials.setUsername(request.getAuthType() == AuthType.BASIC ? request.getUsername().trim() : null);
        credentials.setEncryptedSecret(secretCipher.encrypt(request.getSecret()));
        return CredentialsResponse.from(projectCredentialRepository.saveAndFlush(credentials));
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialsResponse get(Long projectId, User owner) {
        findOwnedProject(projectId, owner);
        return projectCredentialRepository.findByProjectId(projectId)
                .map(CredentialsResponse::from)
                .orElseGet(CredentialsResponse::none);
    }

    @Override
    public void delete(Long projectId, User owner) {
        findOwnedProject(projectId, owner);
        projectCredentialRepository.deleteByProjectId(projectId);
    }

    // ----- helpers ------

    private Project findOwnedProject(Long projectId, User owner){
        return projectRepository.findByIdAndOwnerId(projectId,owner.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Project not found with id: "+projectId));
    }

    private void validate(CredentialRequest req){
        String secret = req.getSecret();
        if (secret.contains("\r") || secret.contains("\n")){
            throw new BadRequestException("Secret must not contain line breaks");
        }
        switch (req.getAuthType()){
            case API_KEY -> {
                String h = req.getHeaderName() == null ? "" : req.getHeaderName().trim();
                if (!HEADER_NAME.matcher(h).matches()){
                    throw new BadRequestException(
                            "API_KEY requires a headerName (letters, digits and hyphens only"
                    );
                }
                if (FORBIDDEN_HEADERS.contains(h.toLowerCase())){
                    throw new BadRequestException("Header '"+h+"' cannot be used for credentials");
                }
            }
            case BASIC -> {
                String u = req.getUsername();
                if (u == null || u.isBlank() || u.contains(":") || u.contains("\r") || u.contains("\n")){
                    throw new BadRequestException(
                            "BASIC requires a username without ':' or line breaks"
                    );
                }
            }
            case BEARER -> {
                if (secret.chars().anyMatch(Character::isWhitespace)){
                    throw new BadRequestException("Bearer token must not contain whitespace");
                }
            }
        }
    }
}
