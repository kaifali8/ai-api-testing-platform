package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.ProjectCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProjectCredentialRepository extends JpaRepository<ProjectCredentials,Long> {

    Optional<ProjectCredentials> findByProjectId(Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
