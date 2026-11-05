package com.assqp.platform.repository;

import com.assqp.platform.entity.QualityAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualityAnalysisRepository extends JpaRepository<QualityAnalysis, Long> {
    List<QualityAnalysis> findByProjectIdOrderByAnalyzedAtDesc(Long projectId);
    Optional<QualityAnalysis> findTopByProjectIdOrderByAnalyzedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
