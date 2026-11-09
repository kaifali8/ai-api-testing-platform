package com.kaif.apitestplatform.entity;

import com.kaif.apitestplatform.enums.TestCategory;
import com.kaif.apitestplatform.enums.TestSource;
import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.units.qual.C;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "suite_id",nullable = false)
    private TestSuite suite;

    private Long endpointId;

    @Column(nullable = false,length = 10)
    private String httpMethod;

    @Column(nullable = false,length = 500)
    private String path;

    @Column(nullable = false,length = 300)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private TestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private TestSource source;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String requestData;

    @Column(nullable = false,length = 50)
    private String expectedStatuses;

    @Column(nullable = false)
    private boolean includeAuth;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreated(){
        createdAt=LocalDateTime.now();
    }

}
