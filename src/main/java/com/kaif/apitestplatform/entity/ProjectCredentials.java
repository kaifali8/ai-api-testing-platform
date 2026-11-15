package com.kaif.apitestplatform.entity;

import com.kaif.apitestplatform.enums.AuthType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "project_id",nullable = false,unique = true)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private AuthType authType;

    @Column(length = 100)
    private String headerName;

    @Column(length = 200)
    private String username;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String encryptedSecret;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch(){
        updatedAt=LocalDateTime.now();
    }
}
