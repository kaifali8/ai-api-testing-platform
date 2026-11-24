package com.kaif.apitestplatform.report;

import com.kaif.apitestplatform.enums.Severity;

public record SeverityRow(Long resultId, Severity severity, String likelyCause) {}