package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.AiAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    Optional<AiAnalysis> findByResultId(Long resultId);

    Page<AiAnalysis> findByExecutionId(Long executionId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("delete from AiAnalysis a where a.executionId in "
            + "(select e.id from TestExecution e where e.projectId = :projectId)")
    void deleteByProjectId(@Param("projectId") Long projectId);
}