package com.kaif.apitestplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "api_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "project_id",nullable = false)
    private Project project;

    @Column(nullable = false,length = 500)
    private String path;

    @Column(nullable = false,length = 10)
    private String httpMethod;


    private String operationId;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean requiresAuth;

    @Column(columnDefinition = "TEXT")
    private String requestBodySchema;

    @Column(columnDefinition = "TEXT")
    private String responseSchemas;

    @OneToMany(mappedBy = "apiDefinition",cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<ApiParameter> parameters=new ArrayList<>();
}
