package com.assqp.platform.repository;

import com.assqp.platform.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findByTestCaseIdOrderByCreatedAtDesc(Long testCaseId);
    List<TestExecution> findByTestCaseProjectIdOrderByCreatedAtDesc(Long projectId);

    void deleteByTestCaseProjectId(Long projectId);

    // Phase 12 audit fix: needed so a single TestCase can be deleted without
    // hitting the same FK-constraint issue Project deletion had - see
    // TestCaseServiceImpl.delete().
    void deleteByTestCaseId(Long testCaseId);
}
