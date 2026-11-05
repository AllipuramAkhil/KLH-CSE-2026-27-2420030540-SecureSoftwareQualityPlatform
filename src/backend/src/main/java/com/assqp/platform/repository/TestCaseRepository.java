package com.assqp.platform.repository;

import com.assqp.platform.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    Page<TestCase> findByProjectId(Long projectId, Pageable pageable);
    List<TestCase> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
