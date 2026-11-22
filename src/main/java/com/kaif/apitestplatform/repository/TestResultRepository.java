package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.TestResult;
import com.kaif.apitestplatform.enums.ResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    Page<TestResult> findByExecutionId(Long executionId, Pageable pageable);

    Page<TestResult> findByExecutionIdAndStatus(Long executionId, ResultStatus status, Pageable pageable);

    // One bulk DELETE instead of loading thousands of rows
    @Transactional
    @Modifying
    @Query("delete from TestResult r where r.executionId in "
            + "(select e.id from TestExecution e where e.projectId = :projectId)")
    void deleteByProjectId(@Param("projectId") Long projectId);

    Optional<TestResult> findByIdAndExecutionId(Long id, Long executionId);

    @Query("select r from TestResult r where r.executionId = :executionId and r.status = :status "
            + "and not exists (select 1 from AiAnalysis a where a.resultId = r.id) order by r.id")
    List<TestResult> findUnanalyzed(@Param("executionId") Long executionId,
                                    @Param("status") ResultStatus status, Pageable pageable);
}