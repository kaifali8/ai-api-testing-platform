package com.kaif.apitestplatform.service;

import com.kaif.apitestplatform.dto.response.ApiDefinitionResponse;
import com.kaif.apitestplatform.dto.response.ImportSummaryResponse;
import com.kaif.apitestplatform.dto.response.PageResponse;
import com.kaif.apitestplatform.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SpecImportService {
    ImportSummaryResponse importSpec(Long projectId, MultipartFile file, User user);
    PageResponse<ApiDefinitionResponse> listEndpoints(Long projectId, User owner, Pageable pageable);
    ApiDefinitionResponse getEndpoint(Long projectId,Long endpointId,User owner);
}
