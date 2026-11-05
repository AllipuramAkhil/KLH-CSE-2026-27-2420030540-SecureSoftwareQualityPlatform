package com.assqp.platform.repository;

import com.assqp.platform.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    List<RiskAssessment> findByProjectIdOrderByAssessedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
