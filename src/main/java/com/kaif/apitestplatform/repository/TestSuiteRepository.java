package com.kaif.apitestplatform.repository;

import com.kaif.apitestplatform.entity.TestSuite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface TestSuiteRepository extends JpaRepository<TestSuite,Long> {

    Page<TestSuite> findByProjectId(Long projectId, Pageable pageable);

    Optional<TestSuite> findByIdAndProjectId(Long id, Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
