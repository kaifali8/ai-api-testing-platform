package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.ApiDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ApiDefinitionRepository extends JpaRepository<ApiDefinition,Long> {

    Page<ApiDefinition> findByProjectId(Long projectId, Pageable pageable);

    Optional<ApiDefinition> findByIdAndProjectId(Long id,Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
