package com.kaif.apitestplatform.dto.response;

import java.util.List;

public record ImportSummaryResponse(
        String specTitle, String specVersion,
        int endpointsImported, int parametersImported,
        List<String> warnings) {

}
