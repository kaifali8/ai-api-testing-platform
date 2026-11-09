package com.kaif.apitestplatform.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_suites")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "project_id",nullable = false)
    private  Project project;

    @Column(nullable = false,length = 100)
    private String name;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "suite",cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    @PrePersist
    private void onCreate(){
        createdAt = LocalDateTime.now();
    }
}
