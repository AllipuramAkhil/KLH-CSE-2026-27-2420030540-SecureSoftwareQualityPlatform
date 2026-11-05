package com.assqp.platform.repository;

import com.assqp.platform.entity.CodeChurnMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeChurnMetricsRepository extends JpaRepository<CodeChurnMetrics, Long> {
    List<CodeChurnMetrics> findByProjectIdOrderByTotalLinesChangedDesc(Long projectId);

    Optional<CodeChurnMetrics> findByProjectIdAndComponentName(Long projectId, String componentName);

    void deleteByProjectId(Long projectId);
}
