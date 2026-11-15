package com.kaif.apitestplatform.entity;

import com.kaif.apitestplatform.enums.ResultStatus;
import com.kaif.apitestplatform.enums.TestCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results",
        indexes = @Index(name = "idx_result_execution", columnList = "execution_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    // Informational only: the test may be edited or deleted later
    private Long testCaseId;

    // Snapshot of what was tested
    @Column(nullable = false, length = 300)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCategory category;

    @Column(nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false, length = 50)
    private String expectedStatuses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultStatus status;

    private Integer actualStatus;
    private Long responseTimeMs;

    @Column(columnDefinition = "TEXT")
    private String requestUrl;

    @Column(columnDefinition = "TEXT")
    private String requestHeaders;      // JSON, credentials masked

    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column(columnDefinition = "TEXT")
    private String responseHeaders;     // JSON, sensitive values masked

    @Column(columnDefinition = "TEXT")
    private String responseBody;        // capped, secrets masked

    private boolean responseTruncated;

    @Column(columnDefinition = "TEXT")
    private String failureReasons;      // one reason per line

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime executedAt;
}