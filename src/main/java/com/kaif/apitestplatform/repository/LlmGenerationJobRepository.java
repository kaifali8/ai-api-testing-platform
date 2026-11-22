package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.LlmGenerationJob;
import com.kaif.apitestplatform.enums.RunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LlmGenerationJobRepository extends JpaRepository<LlmGenerationJob, Long> {

    Page<LlmGenerationJob> findByProjectId(Long projectId, Pageable pageable);

    Optional<LlmGenerationJob> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndStatusIn(Long projectId, Collection<RunStatus> statuses);

    List<LlmGenerationJob> findByStatusIn(Collection<RunStatus> statuses);

    @Transactional
    void deleteByProjectId(Long projectId);
}