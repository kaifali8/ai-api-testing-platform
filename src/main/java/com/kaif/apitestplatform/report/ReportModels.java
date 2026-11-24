package com.kaif.apitestplatform.report;

import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.enums.RunStatus;
import com.kaif.apitestplatform.enums.Severity;
import com.kaif.apitestplatform.enums.TestCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Plain data shapes returned by the reporting endpoints. */
public final class ReportModels {

    private ReportModels() {}

    public record RunInfo(Long id, Long suiteId, String suiteName, RunStatus status,
                          LocalDateTime startedAt, LocalDateTime finishedAt) {}

    public record Totals(int total, int passed, int failed, int errored, double passRate,
                         Double avgResponseTimeMs, Long p95ResponseTimeMs, Long maxResponseTimeMs) {}

    /** One group (a category, an endpoint or a test source). passRate = passed / total, in percent. */
    public record Stats(String key, int total, int passed, int failed, int errored,
                        double passRate, Double avgResponseTimeMs) {}

    public record Finding(Long resultId, String testName, String method, String path,
                          Severity severity, String likelyCause) {}

    public record RunAnalytics(RunInfo run, Totals totals,
                               List<Stats> byCategory, List<Stats> byEndpoint, List<Stats> bySource,
                               List<Stats> slowestEndpoints,
                               Map<String, Integer> failuresBySeverity,
                               List<Finding> topFindings) {}

    public record Change(String testName, String method, String path, TestCategory category,
                         ResultStatus baselineStatus, ResultStatus currentStatus,
                         Integer baselineActualStatus, Integer currentActualStatus,
                         Long baselineTimeMs, Long currentTimeMs, String currentReason) {}

    public record RunComparison(RunInfo baseline, RunInfo current,
                                Totals baselineTotals, Totals currentTotals,
                                double passRateDelta, String verdict,
                                int regressionCount, int fixedCount,
                                int stillFailingCount, int stillPassingCount,
                                int newTestCount, int removedTestCount,
                                List<Change> regressions, List<Change> fixed, List<Change> slower) {}

    public record ReportResult(Long id, String testName, TestCategory category, String source,
                               String method, String path, String expectedStatuses,
                               ResultStatus status, Integer actualStatus, Long responseTimeMs,
                               List<String> failureReasons, String errorMessage,
                               Severity aiSeverity, String aiLikelyCause) {}

    public record RunReport(LocalDateTime generatedAt, RunAnalytics analytics, List<ReportResult> results) {}

    public record TrendPoint(Long runId, String suiteName, LocalDateTime finishedAt,
                             int total, int passed, int failed, int errored, double passRate) {}

    public record ProjectOverview(long endpoints, long suites, List<TrendPoint> trend) {}

    public record SourceCategoryCount(String source, String category, long count) {}

    public record SuiteStats(Long suiteId, String name, long total, long enabled,
                             Map<String, Long> bySource, List<SourceCategoryCount> bySourceAndCategory) {}
}