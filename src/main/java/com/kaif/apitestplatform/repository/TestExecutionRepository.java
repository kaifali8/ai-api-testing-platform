package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.TestExecution;
import com.kaif.apitestplatform.enums.RunStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution,Long> {
    Page<TestExecution> findByProjectId(Long projectId, Pageable pageable);

    Optional<TestExecution> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndStatusIn(Long projectId, Collection<RunStatus> statuses);

    List<TestExecution> findByStatusIn(Collection<RunStatus> statuses);

    @Transactional
    void deleteByProjectId(Long projectId);
}
