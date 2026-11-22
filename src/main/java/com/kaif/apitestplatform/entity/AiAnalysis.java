package com.kaif.apitestplatform.entity;

import com.kaif.apitestplatform.enums.Confidence;
import com.kaif.apitestplatform.enums.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analyses",
        uniqueConstraints = @UniqueConstraint(columnNames = "result_id"),
        indexes = @Index(name = "idx_ai_execution", columnList = "execution_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String likelyCause;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Confidence confidence;

    @Column(columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(columnDefinition = "TEXT")
    private String actualBehavior;

    @Column(columnDefinition = "TEXT")
    private String remediation;

    @Column(length = 100)
    private String model;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}