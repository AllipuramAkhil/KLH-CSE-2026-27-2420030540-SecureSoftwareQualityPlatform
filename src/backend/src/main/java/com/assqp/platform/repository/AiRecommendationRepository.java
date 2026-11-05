package com.assqp.platform.repository;

import com.assqp.platform.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {
    List<AiRecommendation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
