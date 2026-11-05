package com.assqp.platform.repository;

import com.assqp.platform.entity.MlPredictionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MlPredictionLogRepository extends JpaRepository<MlPredictionLog, Long> {
    List<MlPredictionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<MlPredictionLog> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    void deleteByProjectId(Long projectId);
}
