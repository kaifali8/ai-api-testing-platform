package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.dto.response.ApiDefinitionResponse;
import com.kaif.apitestplatform.dto.response.ImportSummaryResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.service.SpecImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class SpecController {
    private final SpecImportService specImportService;

    @PostMapping(value = "/spec/import",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSummaryResponse> importSpec(
            @PathVariable Long projectId,
            @RequestParam("file")MultipartFile file,
            @AuthenticationPrincipal User user){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(specImportService.importSpec(projectId, file, user));
    }

    @GetMapping("/endpoints")
    public ResponseEntity<PageResponse<ApiDefinitionResponse>> list(
            @PathVariable Long projectId,
            @PageableDefault(size = 20,sort = "path",direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(specImportService.listEndpoints(projectId,user,pageable));
    }

    @GetMapping("/endpoints/{endpointId}")
    public ResponseEntity<ApiDefinitionResponse> get(
        @PathVariable Long projectId,
        @PathVariable Long endpointId,
        @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(specImportService.getEndpoint(projectId, endpointId, user));
    }
}
