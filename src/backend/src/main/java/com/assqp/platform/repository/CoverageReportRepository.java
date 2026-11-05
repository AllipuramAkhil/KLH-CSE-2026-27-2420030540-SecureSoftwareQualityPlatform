package com.assqp.platform.repository;

import com.assqp.platform.entity.CoverageReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverageReportRepository extends JpaRepository<CoverageReport, Long> {
    List<CoverageReport> findByProjectIdOrderByImportedAtDesc(Long projectId);

    Optional<CoverageReport> findTopByProjectIdOrderByImportedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
