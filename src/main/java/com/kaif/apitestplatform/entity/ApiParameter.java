package com.kaif.apitestplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "api_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "api_definition_id",nullable = false)
    private ApiDefinition apiDefinition;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,length = 10)
    private String location;

    private boolean required;

    private String type;
    private String format;

    private BigDecimal minimum;
    private BigDecimal maximum;
    private Integer minLength;
    private Integer maxLength;

    @Column(length = 500)
    private String pattern;

    @Column(length = 1000)
    private String enumValues;

    @Column(length = 1000)
    private String description;
}
