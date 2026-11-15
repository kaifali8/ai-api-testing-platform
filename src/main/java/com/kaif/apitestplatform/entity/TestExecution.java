package com.kaif.apitestplatform.entity;

import com.kaif.apitestplatform.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions",
        indexes = @Index(name = "idx_exec_project", columnList = "project_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "suite_id", nullable = false)
    private Long suiteId;

    @Column(nullable = false, length = 100)
    private String suiteName;

    // Snapshot of the project's base URL at the moment the run started
    @Column(nullable = false, length = 500)
    private String baseUrl;

    private Long triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    private int totalTests;
    private int passed;
    private int failed;
    private int errored;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}