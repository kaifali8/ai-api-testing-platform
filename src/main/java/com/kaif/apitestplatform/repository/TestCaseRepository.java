package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.TestCase;
import com.kaif.apitestplatform.enums.TestCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase,Long> {

    Page<TestCase> findBySuiteId(Long suiteId, Pageable pageable);

    Page<TestCase> findBySuiteIdAndCategory(Long suiteId, TestCategory category,Pageable pageable);

    long countBySuiteId(Long suiteId);
}
