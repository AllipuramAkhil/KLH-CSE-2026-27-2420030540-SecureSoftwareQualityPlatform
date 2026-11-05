package com.assqp.platform.repository;

import com.assqp.platform.entity.SonarQubeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SonarQubeAnalysisRepository extends JpaRepository<SonarQubeAnalysis, Long> {
    List<SonarQubeAnalysis> findByProjectIdOrderByImportedAtDesc(Long projectId);

    Optional<SonarQubeAnalysis> findTopByProjectIdOrderByImportedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
