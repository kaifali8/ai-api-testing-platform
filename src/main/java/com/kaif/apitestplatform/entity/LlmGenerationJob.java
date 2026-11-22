package com.kaif.apitestplatform.entity;

import com.kaif.apitestplatform.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_generation_jobs",
        indexes = @Index(name = "idx_llmjob_project", columnList = "project_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmGenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "suite_id", nullable = false)
    private Long suiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    private int maxPerEndpoint;
    private int endpointsTotal;
    private int endpointsProcessed;
    private int endpointsFailed;

    // Research counters: proposed = accepted + duplicates + invalid
    private int proposed;
    private int accepted;
    private int duplicates;
    private int invalid;

    // JSON object: {"unknown_query_param": 3, "bad_status": 1}
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String rejectionReasons = "{}";

    @Column(length = 100)
    private String model;

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