package com.kaif.apitestplatform.dto.response;

public record AnalysisBatchResponse(int queued, int maxPerRequest, String note) {}